import time
import subprocess
import logging
import docker
from hazelcast.cluster import MemberInfo
from hazelcast.partition import PartitionService
from .test import Test

logger = logging.getLogger(__name__)

class FailoverTest(Test):
    """Test class for verifying failover capabilities and recovery time in Hazelcast cluster"""

    members: list[MemberInfo]
    """List of Hazelcast cluster members"""

    partition_service: PartitionService
    """Partition service to manage data distribution across members"""
    
    def __init__(self, test_info: str):
        super().__init__("failover_test", test_info)
        self.docker_client = docker.from_env()
        
    def run_test(self):
        """Test node failure and failover capability with recovery time analysis"""
        try:
            self.partition_service = self.client.partition_service
            
            self.members = self.client.cluster_service.get_members()
            data_sizes = [0, 10000, 100000, 1000000]
            
            logger.info("Starting failover test with different data sizes...")
            logger.info("Starting with big data sizes...")
            self.failover_test(data_sizes, True)
            logger.info("Starting with smaller data sizes...")
            self.failover_test(data_sizes, False)

            return True
        except Exception as e:
            logger.error(f"Failover test failed: {e}")
            self.report.add_result("failover_test", "FAIL", str(e))
            return False

    def custom_teardown(self):
        distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
        distributed_map.clear()
        distributed_map.destroy()

    def get_container_stats(self, container):
        """Get current disk and network stats for a container"""
        try:
            stats = container.stats(stream=False)
            
            # Get network I/O stats
            network_rx = 0
            network_tx = 0
            if 'networks' in stats:
                for _, net_stats in stats['networks'].items():
                    network_rx += net_stats.get('rx_bytes', 0)
                    network_tx += net_stats.get('tx_bytes', 0)
            
            return {
                'network_rx': network_rx,
                'network_tx': network_tx
            }
        except Exception as e:
            logger.error(f"Failed to get stats for container {container.name}: {e}")
            return {'disk_read': 0, 'disk_write': 0, 'network_rx': 0, 'network_tx': 0}

    def get_cluster_stats(self):
        """Get disk and network stats for all Hazelcast containers"""
        client = docker.from_env()
        containers = client.containers.list(filters={"name": "hazelcast"})
        cluster_stats = {}
        
        for container in containers:
            cluster_stats[container.name] = self.get_container_stats(container)
        
        return cluster_stats

    def calculate_stats_delta(self, before_stats, after_stats):
        """Calculate the difference in stats between two measurements"""
        delta_stats = {}
        
        for container_name in before_stats:
            if container_name in after_stats:
                delta_stats[container_name] = {
                    'network_rx_delta': after_stats[container_name]['network_rx'] - before_stats[container_name]['network_rx'],
                    'network_tx_delta': after_stats[container_name]['network_tx'] - before_stats[container_name]['network_tx']
                }
        
        return delta_stats

    def failover_test(self, data_sizes, big_value):
        for size in data_sizes:
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
            distributed_map.clear()
            if not distributed_map.destroy():
                logger.warning("Failed to destroy existing map")

            objs = self.client.get_distributed_objects()
            # Ensure all distributed objects are cleaned up
            for obj in objs:
                try:
                    obj.destroy()
                except Exception as e:
                    logger.warning(f"Error destroying object {obj}: {e}")

            logger.info(f"Running failover test with {size} items...")
            
            # Get map and store data
            start = time.time_ns()
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
            time_to_retrive_map = time.time_ns() - start
            distributed_map.clear()
            
            # Store test data
            logger.info(f"\tStoring {size} items in the distributed map...")
            if big_value:
                data = {f"key{i}": "x" * 1023 + str(i) for i in range(size)}
            else:
                data = {f"key{i}": f"value{i}" for i in range(size)}
            
            distributed_map.put_all(data)
            del data
            
            assert distributed_map.size() == size, f"Expected {size} items in the map, found {distributed_map.size()}"
            
            # Get baseline stats before failure
            logger.info("\tGetting baseline cluster stats before failure...")
            baseline_stats = self.get_cluster_stats()
            
            # Kill one node and start measuring recovery time
            logger.info("\tStopping hazelcast-node1...")
            failure_start_time = time.time_ns()
            subprocess.run(["docker", "stop", "hazelcast-node1"], check=True)
            
            map = self.client.get_map("mappa-distribuita-1").blocking()
            
            while size != map.size():
                continue

            time_to_recover = time.time_ns() - failure_start_time - time_to_retrive_map

            time.sleep(5)
            
            # Get stats after failure recovery
            logger.info("\tGetting cluster stats after failure recovery...")
            after_failure_stats = self.get_cluster_stats()
            failure_delta_stats = self.calculate_stats_delta(baseline_stats, after_failure_stats)

            if big_value:
                data_size_str = "big_data_size"
            else:
                data_size_str = "small_data_size"

            logger.info(f"\tNode failure detected, recovery time: {time_to_recover} nano seconds")
            self.report.add_metric(f"{data_size_str}_recovery_time_nano_seconds_{size}", time_to_recover)
            self.report.add_result(f"{data_size_str}_failover_recovery_time_{size}", "PASS", f"Recovery time for {size} items: {time_to_recover} nano seconds")

            # Log and record failure delta stats
            for container_name, delta in failure_delta_stats.items():
                logger.info(f"\t{container_name} during failure recovery - Network RX/TX: {delta['network_rx_delta']}/{delta['network_tx_delta']} bytes")
                self.report.add_metric(f"{data_size_str}_failure_network_rx_delta_{container_name}_{size}", delta['network_rx_delta'])
                self.report.add_metric(f"{data_size_str}_failure_network_tx_delta_{container_name}_{size}", delta['network_tx_delta'])
            
            # Get stats before rejoin
            before_rejoin_stats = self.get_cluster_stats()
            
            # Restart the node and measure rejoin time
            logger.info("\tRestarting hazelcast-node1...")
            rejoin_start_time = time.time_ns()
            subprocess.run(["docker", "start", "hazelcast-node1"], check=True)

            map = self.client.get_map("mappa-distribuita-1").blocking()

            while size != map.size():
                continue

            time_after_rejoin = time.time_ns() - rejoin_start_time - time_to_retrive_map

            time.sleep(5)
            
            # Get stats after rejoin
            logger.info("\tGetting cluster stats after node rejoin...")
            after_rejoin_stats = self.get_cluster_stats()
            rejoin_delta_stats = self.calculate_stats_delta(before_rejoin_stats, after_rejoin_stats)
            
            logger.info(f"\tNode rejoined, rejoin time: {time_after_rejoin} nano seconds")
            self.report.add_metric(f"{data_size_str}_rejoin_time_nano_seconds_{size}", time_after_rejoin)
            self.report.add_result(f"{data_size_str}_failover_rejoin_time_{size}", "PASS", f"Rejoin time for {size} items: {time_after_rejoin} nano seconds")

            # Log and record rejoin delta stats
            for container_name, delta in rejoin_delta_stats.items():
                logger.info(f"\t{container_name} during rejoin - Network RX/TX: {delta['network_rx_delta']}/{delta['network_tx_delta']} bytes")
                self.report.add_metric(f"{data_size_str}_rejoin_network_rx_delta_{container_name}_{size}", delta['network_rx_delta'])
                self.report.add_metric(f"{data_size_str}_rejoin_network_tx_delta_{container_name}_{size}", delta['network_tx_delta'])

            # Calculate total I/O impact
            total_network_rx = sum(delta['network_rx_delta'] for delta in failure_delta_stats.values()) + sum(delta['network_rx_delta'] for delta in rejoin_delta_stats.values())
            total_network_tx = sum(delta['network_tx_delta'] for delta in failure_delta_stats.values()) + sum(delta['network_tx_delta'] for delta in rejoin_delta_stats.values())

            logger.info(f"\tTotal I/O impact for {size} items failover: Network RX/TX: {total_network_rx}/{total_network_tx} bytes")
            self.report.add_metric(f"{data_size_str}_total_network_rx_impact_{size}", total_network_rx)
            self.report.add_metric(f"{data_size_str}_total_network_tx_impact_{size}", total_network_tx)

            self.report.add_result(f"{data_size_str}_failover_test_{size}", "PASS", f"Failover test with {size} items completed successfully")
