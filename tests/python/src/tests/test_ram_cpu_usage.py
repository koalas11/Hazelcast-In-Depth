import logging
import random
import threading
import time
import docker
import hazelcast

from .test import Test

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

class RamCPUUsageTest(Test):
    """Test class for verifying failover capabilities in Hazelcast cluster"""
    
    def __init__(self, test_info: str):
        super().__init__("ram_cpu_test", test_info)

    def run_test(self) -> bool:
        """Test hazelcast usage operations"""
        try:
            logger.info("Starting usage test...")
            self.track_idle_usage()
            logger.info("Usage test passed successfully.")
            self.report.add_result("usage_tracking", "PASS", "Usage test passed successfully")

            return True
        except Exception as e:
            logger.error(f"Map test failed: {e}")
            self.report.add_result("usage_tracking", "FAIL", str(e))
        return False

    def custom_teardown(self):
        distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
        distributed_map.clear()
        distributed_map.destroy()

    def track_idle_usage(self, durations=[300], data_sizes=[0, 10000, 100000, 1000000]):
        """
        Track RAM and CPU usage of the Hazelcast cluster in idle state with different data loads
        using the Docker Python package.
        
        Args:
            durations: List of durations (in seconds) to monitor for each data size
            data_sizes: List of data sizes (number of entries) to load into the cluster
        """
        logger.info("Testing RAM and CPU usage during idle state...")
        
        client = docker.from_env()
        
        results = {}
        
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

            logger.info(f"Preparing cluster with {size} entries...")

            # Clear existing data
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
            distributed_map.clear()
            
            # Populate map with test data
            if size > 0:
                logger.info(f"Populating map with {size} entries...")
                data = {f"key_{i}": "x" * 1023 + str(i) for i in range(size)}
                distributed_map.put_all(data)
                logger.info(f"Map populated with {distributed_map.size()} entries")
                del data
            
            # Let the cluster stabilize
            time.sleep(30)
            
            size_results = {}
            
            for duration in durations:
                logger.info(f"Monitoring resource usage with {size} entries for {duration} seconds...")
                
                # Get Hazelcast containers
                hz_containers: list = client.containers.list(filters={"name": "hazelcast"})
                if not hz_containers:
                    logger.warning("No Hazelcast containers found")
                    continue
                    
                start_time = time.time()
                container_samples = {container.id: [] for container in hz_containers}
                previous_network_stats = {}
                
                # Sample resource usage during the specified duration
                while time.time() - start_time < duration:
                    for container in hz_containers:
                        try:
                            # Get container stats (streaming)
                            stats = container.stats(stream=False)
                            
                            # Calculate CPU usage percentage
                            cpu_delta = stats['cpu_stats']['cpu_usage']['total_usage'] - \
                                        stats['precpu_stats']['cpu_usage']['total_usage']
                            system_delta = stats['cpu_stats']['system_cpu_usage'] - \
                                        stats['precpu_stats']['system_cpu_usage']
                            cpu_usage = stats['cpu_stats']['cpu_usage']
                            if 'percpu_usage' in cpu_usage and isinstance(cpu_usage['percpu_usage'], list):
                                cpu_count = len(cpu_usage['percpu_usage'])
                            else:
                                cpu_count = 8  # Fallback if not present
                            
                            cpu_percent = 0.0
                            if system_delta > 0 and cpu_delta > 0:
                                cpu_percent = (cpu_delta / system_delta) * cpu_count * 100.0
                            
                            # Calculate memory usage percentage
                            mem_usage = stats['memory_stats']['usage']
                            mem_limit = stats['memory_stats']['limit']
                            mem_percent = (mem_usage / mem_limit) * 100.0
                            
                            # Calculate disk I/O usage
                            disk_read = 0
                            disk_write = 0
                            if 'blkio_stats' in stats and 'io_service_bytes_recursive' in stats['blkio_stats']:
                                for io_stat in stats['blkio_stats']['io_service_bytes_recursive']:
                                    if io_stat['op'] == 'Read':
                                        disk_read += io_stat['value']
                                    elif io_stat['op'] == 'Write':
                                        disk_write += io_stat['value']
                            
                            # Calculate network I/O usage (cumulative)
                            network_rx_total = 0
                            network_tx_total = 0
                            if 'networks' in stats:
                                for interface, net_stats in stats['networks'].items():
                                    network_rx_total += net_stats.get('rx_bytes', 0)
                                    network_tx_total += net_stats.get('tx_bytes', 0)
                            
                            # Calculate network deltas from previous measurement
                            network_rx = 0
                            network_tx = 0
                            
                            if container.id in previous_network_stats:
                                network_rx = network_rx_total - previous_network_stats[container.id]['network_rx_total']
                                network_tx = network_tx_total - previous_network_stats[container.id]['network_tx_total']
                            
                            # Store current totals for next iteration
                            previous_network_stats[container.id] = {
                                'network_rx_total': network_rx_total,
                                'network_tx_total': network_tx_total
                            }
                            
                            container_samples[container.id].append({
                                'mem_usage': mem_usage,
                                'mem_usage_perc': mem_percent,
                                'cpu_usage': cpu_percent,
                                'disk_read': disk_read,
                                'disk_write': disk_write,
                                'network_rx': network_rx,
                                'network_tx': network_tx,
                            })
                            
                            logger.debug(f"Container {container.id[:12]}: Memory: {mem_usage}, Memory Perc: {mem_percent:.2f}%, "
                                        f"CPU: {cpu_percent:.2f}%, Disk R/W: {disk_read}/{disk_write} bytes, "
                                        f"Network RX/TX: {network_rx}/{network_tx} bytes")
                            
                        except Exception as e:
                            logger.error(f"Failed to get stats for container {container.id[:12]}: {e}")
                    
                    time.sleep(5)  # Sample every 5 seconds
                
                # Calculate averages for each container and overall
                container_averages = {}
                all_mem_samples = []
                all_men_perc_samples = []
                all_cpu_samples = []
                all_disk_read_samples = []
                all_disk_write_samples = []
                all_network_rx_samples = []
                all_network_tx_samples = []

                for container_id, samples in container_samples.items():
                    if samples:
                        avg_mem = sum(sample['mem_usage'] for sample in samples) / len(samples)
                        avg_mem_perc = sum(sample['mem_usage_perc'] for sample in samples) / len(samples)
                        avg_cpu = sum(sample['cpu_usage'] for sample in samples) / len(samples)
                        avg_disk_read = sum(sample['disk_read'] for sample in samples) / len(samples)
                        avg_disk_write = sum(sample['disk_write'] for sample in samples) / len(samples)
                        avg_network_rx = sum(sample['network_rx'] for sample in samples) / len(samples)
                        avg_network_tx = sum(sample['network_tx'] for sample in samples) / len(samples)
                        
                        container_averages[container_id] = {
                            'avg_mem_usage': avg_mem,
                            'avg_mem_perc': avg_mem_perc,
                            'avg_cpu_usage': avg_cpu,
                            'avg_disk_read': avg_disk_read,
                            'avg_disk_write': avg_disk_write,
                            'avg_network_rx': avg_network_rx,
                            'avg_network_tx': avg_network_tx
                        }
                        
                        all_mem_samples.extend([sample['mem_usage'] for sample in samples])
                        all_men_perc_samples.extend([sample['mem_usage_perc'] for sample in samples])
                        all_cpu_samples.extend([sample['cpu_usage'] for sample in samples])
                        all_disk_read_samples.extend([sample['disk_read'] for sample in samples])
                        all_disk_write_samples.extend([sample['disk_write'] for sample in samples])
                        all_network_rx_samples.extend([sample['network_rx'] for sample in samples])
                        all_network_tx_samples.extend([sample['network_tx'] for sample in samples])
                        
                        logger.info(f"Container {container_id[:12]} with {size} entries over {duration}s: "
                                f"Avg Memory: {avg_mem}, Avg Memory Perc: {avg_mem_perc:.2f}%, Avg CPU: {avg_cpu:.2f}%, "
                                f"Avg Disk R/W: {avg_disk_read:.0f}/{avg_disk_write:.0f} bytes, "
                                f"Avg Network RX/TX: {avg_network_rx:.0f}/{avg_network_tx:.0f} bytes")
                
                # Calculate overall averages
                overall_avg_mem = sum(all_mem_samples) / len(all_mem_samples) if all_mem_samples else 0
                overall_avg_mem_perc = sum(all_men_perc_samples) / len(all_men_perc_samples) if all_men_perc_samples else 0
                overall_avg_cpu = sum(all_cpu_samples) / len(all_cpu_samples) if all_cpu_samples else 0
                overall_avg_disk_read = sum(all_disk_read_samples) / len(all_disk_read_samples) if all_disk_read_samples else 0
                overall_avg_disk_write = sum(all_disk_write_samples) / len(all_disk_write_samples) if all_disk_write_samples else 0
                overall_avg_network_rx = sum(all_network_rx_samples) / len(all_network_rx_samples) if all_network_rx_samples else 0
                overall_avg_network_tx = sum(all_network_tx_samples) / len(all_network_tx_samples) if all_network_tx_samples else 0
                
                size_results[duration] = {
                    'avg_mem_usage': overall_avg_mem,
                    'avg_mem_perc': overall_avg_mem_perc,
                    'avg_cpu_usage': overall_avg_cpu,
                    'avg_disk_read': overall_avg_disk_read,
                    'avg_disk_write': overall_avg_disk_write,
                    'avg_network_rx': overall_avg_network_rx,
                    'avg_network_tx': overall_avg_network_tx,
                    'container_averages': container_averages
                }
                
                logger.info(f"Overall results for {size} entries over {duration}s: "
                        f"Avg Memory: {overall_avg_mem:.0f} bytes, Avg Memory Perc: {overall_avg_mem_perc:.2f}%, Avg CPU: {overall_avg_cpu:.2f}%, "
                        f"Avg Disk R/W: {overall_avg_disk_read:.0f}/{overall_avg_disk_write:.0f} bytes, "
                        f"Avg Network RX/TX: {overall_avg_network_rx:.0f}/{overall_avg_network_tx:.0f} bytes")
            else:
                logger.warning(f"No samples collected for {size} entries over {duration}s duration")
            
            results[size] = size_results
        
        # Add results to the test report
        for size, size_data in results.items():
            for duration, metrics in size_data.items():
                self.report.add_result(
                    f"idle_usage_{size}_entries_{duration}s",
                    "INFO",
                    f"Avg Memory: {metrics['avg_mem_usage']:.0f} bytes, Avg Memory Perc: {metrics['avg_mem_perc']:.2f}%, Avg CPU: {metrics['avg_cpu_usage']:.2f}%, "
                    f"Avg Disk R/W: {metrics['avg_disk_read']:.0f}/{metrics['avg_disk_write']:.0f} bytes, "
                    f"Avg Network RX/TX: {metrics['avg_network_rx']:.0f}/{metrics['avg_network_tx']:.0f} bytes"
                )
                for container_id, averages in metrics['container_averages'].items():
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_mem_usage_{size}_{duration}s",
                        averages['avg_mem_usage']
                    )
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_mem_usage_perc_{size}_{duration}s",
                        averages['avg_mem_perc']
                    )
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_cpu_usage_{size}_{duration}s",
                        averages['avg_cpu_usage']
                    )
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_disk_read_{size}_{duration}s",
                        averages['avg_disk_read']
                    )
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_disk_write_{size}_{duration}s",
                        averages['avg_disk_write']
                    )
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_network_rx_{size}_{duration}s",
                        averages['avg_network_rx']
                    )
                    self.report.add_metric(
                        f"idle_container_{container_id[:12]}_avg_network_tx_{size}_{duration}s",
                        averages['avg_network_tx']
                    )
