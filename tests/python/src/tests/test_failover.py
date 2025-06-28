import time
import subprocess
import logging
from hazelcast.cluster import MemberInfo
from hazelcast.partition import PartitionService
from .test import Test

logger = logging.getLogger(__name__)

class FailoverTest(Test):
    """Test class for verifying failover capabilities and recovery time in Hazelcast cluster"""

    members: list[MemberInfo]

    partition_service: PartitionService
    """Partition service to manage data distribution across members"""
    
    def __init__(self, test_info: str):
        super().__init__("failover_test", test_info)
        
    def run_test(self):
        """Test node failure and failover capability with recovery time analysis"""
        try:
            self.partition_service = self.client.partition_service
            
            self.members = self.client.cluster_service.get_members()
            data_sizes = [0, 1000, 10000, 100000]
            
            logger.info("Starting failover test with different data sizes...")
            logger.info("Starting with big data sizes...")
            self.failover_test(data_sizes, True)
            logger.info("Starting with smaller data sizes...")
            self.failover_test(data_sizes, False)

            return True
        
        except Exception as e:
            logger.error(f"Test failed: {str(e)}")
            self.report.add_result("unexpected_error", "FAIL", f"Error: {str(e)}")
            return False
        
    def failover_test(self, data_sizes, big_value):
        for size in data_sizes:
            objs = self.client.get_distributed_objects()
            # Ensure all distributed objects are cleaned up
            for obj in objs:
                try:
                    obj.destroy()
                except Exception as e:
                    print(f"Error destroying object {obj}: {e}")

            logger.info(f"Running failover test with {size} items...")
            
            # Get map and store data
            start = time.time_ns()
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
            time_to_retrive_map = time.time_ns() - start
            distributed_map.clear()
            
            # Store test data
            logger.info(f"\tStoring {size} items in the distributed map...")
            if big_value:
                data = {f"key{i}": "x" * 1023 + i for i in range(size)}
            else:
                data = {f"key{i}": f"value{i}" for i in range(size)}
            
            distributed_map.put_all(data)
            del data
            
            assert distributed_map.size() == size, f"Expected {size} items in the map, found {distributed_map.size()}"
            
            # Kill one node and start measuring recovery time
            logger.info("\tStopping hazelcast-node1...")
            failure_start_time = time.time_ns()
            subprocess.run(["docker", "stop", "hazelcast-node1"], check=True)
            
            map = self.client.get_map("mappa-distribuita-1").blocking()
            
            while size != map.size():
                continue

            time_to_recover = time.time_ns() - failure_start_time - time_to_retrive_map

            if big_value:
                data_size_str = "big_data_size"
            else:
                data_size_str = "small_data_size"

            logger.info(f"\tNode failure detected, recovery time: {time_to_recover} nano seconds")
            self.report.add_metric(f"{data_size_str}_stringrecovery_time_nano_seconds_{size}", time_to_recover)
            self.report.add_result(f"{data_size_str}_failover_recovery_time_{size}", "PASS", f"Recovery time for {size} items: {time_to_recover} nano seconds")

            time.sleep(5)
            
            # Restart the node and measure rejoin time
            logger.info("\tRestarting hazelcast-node1...")
            rejoin_start_time = time.time_ns()
            subprocess.run(["docker", "start", "hazelcast-node1"], check=True)

            map = self.client.get_map("mappa-distribuita-1").blocking()

            while size != map.size():
                continue

            time_after_rejoin = time.time_ns() - rejoin_start_time - time_to_retrive_map
            
            logger.info(f"\tNode rejoined, rejoin time: {time_after_rejoin} nano seconds")
            self.report.add_metric(f"{data_size_str}_rejoin_time_nano_seconds_{size}", time_after_rejoin)
            self.report.add_result(f"{data_size_str}_failover_rejoin_time_{size}", "PASS", f"Rejoin time for {size} items: {time_after_rejoin} nano seconds")

            time.sleep(5)

            self.report.add_result(f"{data_size_str}_failover_test_{size}", "PASS", f"Failover test with {size} items completed successfully")