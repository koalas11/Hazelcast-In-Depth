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
            # self.track_loaded_usage()
            logger.info("Usage test passed successfully.")
            self.report.add_result("usage_tracking", "PASS", "Usage test passed successfully")

            return True
        except Exception as e:
            logger.error(f"Map test failed: {e}")
            self.report.add_result("usage_tracking", "FAIL", str(e))
        return False

    def track_idle_usage(self, durations=[60, 300], data_sizes=[0, 1000, 10000, 100000]):
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
            objs = self.client.get_distributed_objects()
            # Ensure all distributed objects are cleaned up
            for obj in objs:
                try:
                    obj.destroy()
                except Exception as e:
                    print(f"Error destroying object {obj}: {e}")

            logger.info(f"Preparing cluster with {size} entries...")
            
            # Clear existing data
            test_map = self.client.get_map("test_map").blocking()
            test_map.clear()
            
            # Populate map with test data
            if size > 0:
                logger.info(f"Populating map with {size} entries...")
                data = {f"key_{i}": "x" * 1023 + str(i) for i in range(size)}
                test_map.put_all(data)
                logger.info(f"Map populated with {test_map.size()} entries")
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
                
                # Sample resource usage during the specified duration
                while time.time() - start_time < duration:
                    for container in hz_containers:
                        try:
                            # Get container stats
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
                            
                            container_samples[container.id].append({
                                'mem_usage': mem_usage,
                                'mem_usage_perc': mem_percent,
                                'cpu_usage': cpu_percent,
                                'timestamp': time.time() - start_time
                            })
                            
                            logger.debug(f"Container {container.id[:12]}: Memory: {mem_usage}, Memory Perc: {mem_percent:.2f}%, CPU: {cpu_percent:.2f}%")
                        except Exception as e:
                            logger.error(f"Failed to get stats for container {container.id[:12]}: {e}")
                    
                    time.sleep(5)  # Sample every 5 seconds
                
                # Calculate averages for each container and overall
                container_averages = {}
                all_mem_samples = []
                all_men_perc_samples = []
                all_cpu_samples = []
                
                for container_id, samples in container_samples.items():
                    if samples:
                        avg_mem = sum(sample['mem_usage'] for sample in samples) / len(samples)
                        avg_mem_perc = sum(sample['mem_usage_perc'] for sample in samples) / len(samples)
                        avg_cpu = sum(sample['cpu_usage'] for sample in samples) / len(samples)
                        container_averages[container_id] = {
                            'avg_mem_usage': avg_mem,
                            'avg_mem_perc': avg_mem_perc,
                            'avg_cpu_usage': avg_cpu
                        }
                        all_mem_samples.extend([sample['mem_usage'] for sample in samples])
                        all_men_perc_samples.extend([sample['mem_usage'] for sample in samples])
                        all_cpu_samples.extend([sample['cpu_usage'] for sample in samples])
                        
                        logger.info(f"Container {container_id[:12]} with {size} entries over {duration}s: "
                                f"Avg Memory: {avg_mem}, Avg Memory Perc: {avg_mem_perc:.2f}%, Avg CPU: {avg_cpu:.2f}%")
                
                # Calculate overall averages
                if all_mem_samples and all_cpu_samples:
                    overall_avg_mem = sum(all_mem_samples) / len(all_mem_samples)
                    overall_avg_mem_perc = sum(all_men_perc_samples) / len(all_men_perc_samples)
                    overall_avg_cpu = sum(all_cpu_samples) / len(all_cpu_samples)
                    
                    size_results[duration] = {
                        'avg_mem_usage': overall_avg_mem,
                        'avg_mem_perc': overall_avg_mem_perc,
                        'avg_cpu_usage': overall_avg_cpu,
                        'container_averages': container_averages
                    }
                    
                    logger.info(f"Overall results for {size} entries over {duration}s: "
                            f"Avg Memory: {overall_avg_mem}, Avg Memory Perc: {avg_mem_perc}, Avg CPU: {overall_avg_cpu:.2f}%")
                else:
                    logger.warning(f"No samples collected for {size} entries over {duration}s duration")
            
            results[size] = size_results
        
        # Add results to the test report
        for size, size_data in results.items():
            for duration, metrics in size_data.items():
                self.report.add_result(
                    f"idle_usage_{size}_entries_{duration}s",
                    "INFO",
                    f"Avg Memory: {metrics['avg_mem_usage']}, Avg Memory Perc: {metrics['avg_mem_usage']:.2f}%, Avg CPU: {metrics['avg_cpu_usage']:.2f}%"
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

    def track_loaded_usage(self, durations=[60, 300], data_sizes=[0, 10000, 100000, 1000000, 10000000]):
        """
        Track RAM and CPU usage of the Hazelcast cluster under load with different data sizes
        using the Docker Python package.

        Args:
            durations: List of durations (in seconds) to monitor for each data size
            data_sizes: List of data sizes (number of entries) to load into the cluster
        """
        logger.info("Testing RAM and CPU usage under load...")

        client = docker.from_env()

        results = {}

        for size in data_sizes:
            logger.info(f"Preparing cluster with {size} entries...")

            # Clear existing data
            test_map = self.client.get_map("test_map").blocking()
            test_map.clear()

            # Populate map with test data
            if size > 0:
                logger.info(f"Populating map with {size} entries...")
                batch_size = 1000
                for batch in range(0, size, batch_size):
                    batch_end = min(batch + batch_size, size)
                    batch_data = {f"key_{i}": "x" * 1024 for i in range(batch, batch_end)}
                    test_map.put_all(batch_data)

                logger.info(f"Map populated with {test_map.size()} entries")

            # Let the cluster stabilize
            time.sleep(30)

            size_results = {}

            for duration in durations:
                logger.info(f"Monitoring resource usage with {size} entries for {duration} seconds...")

                # Get Hazelcast containers
                hz_containers = client.containers.list(filters={"name": "hazelcast"})
                if not hz_containers:
                    logger.warning("No Hazelcast containers found")
                    continue

                start_time = time.time()
                container_samples = {container.id: [] for container in hz_containers}

                def load_test_worker(client: hazelcast.HazelcastClient, duration):
                    test_map = client.get_map("test_map")
                    start_time = time.time()

                    data = {f"load_test_{i}": "value" for i in range(1000000)}
                    keys = list(data.keys())

                    while time.time() - start_time < duration:
                        key = f"load_test_1"
                        test_map.put(key, "value")
                        test_map.get(key)
                        test_map.remove(key)
                        test_map.put_all(data)
                        test_map.get_all(keys)
                        test_map.remove_all(keys)
                        test_map.size()
                
                def track_stats(duration):
                    # Sample resource usage during the specified duration
                    while time.time() - start_time < duration:
                        for container in hz_containers:
                            try:
                                # Get container stats
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
                                container_samples[container.id].append({
                                    'mem_usage': mem_percent,
                                    'cpu_usage': cpu_percent,
                                    'timestamp': time.time() - start_time
                                })
                                logger.debug(f"Container {container.id[:12]}: Memory: {mem_percent:.2f}%, CPU: {cpu_percent:.2f}%")
                            except Exception as e:
                                logger.error(f"Failed to get stats for container {container.id[:12]}: {e}")
                        time.sleep(5)

                # Esempio di uso con threading
                threads = []
                for _ in range(3):
                    t = threading.Thread(target=load_test_worker, args=(self.client, duration))
                    t.start()
                    threads.append(t)

                thread_stats = threading.Thread(target=track_stats, args=(duration,))
                thread_stats.start()
                threads.append(thread_stats)
                for t in threads:
                    t.join()

                # Calculate averages for each container and overall

                container_averages = {}
                all_mem_samples = []
                all_cpu_samples = []

                for container_id, samples in container_samples.items():
                    if samples:
                        avg_mem = sum(sample['mem_usage'] for sample in samples) / len(samples)
                        avg_cpu = sum(sample['cpu_usage'] for sample in samples) / len(samples)
                        container_averages[container_id] = {
                            'avg_mem_usage': avg_mem,
                            'avg_cpu_usage': avg_cpu
                        }
                        all_mem_samples.extend([sample['mem_usage'] for sample in samples])
                        all_cpu_samples.extend([sample['cpu_usage'] for sample in samples])
                        logger.info(f"Container {container_id[:12]} with {size} entries over {duration}s: "
                                    f"Avg Memory: {avg_mem:.2f}%, Avg CPU: {avg_cpu:.2f}%")
                        
                # Calculate overall averages
                if all_mem_samples and all_cpu_samples:
                    overall_avg_mem = sum(all_mem_samples) / len(all_mem_samples)
                    overall_avg_cpu = sum(all_cpu_samples) / len(all_cpu_samples)
                    size_results[duration] = {
                        'avg_mem_usage': overall_avg_mem,
                        'avg_cpu_usage': overall_avg_cpu,
                        'container_averages': container_averages
                    }
                    logger.info(f"Overall results for {size} entries over {duration}s: "
                                f"Avg Memory: {overall_avg_mem:.2f}%, Avg CPU: {overall_avg_cpu:.2f}%")
                else:
                    logger.warning(f"No samples collected for {size} entries over {duration}s duration")

            results[size] = size_results

        # Add results to the test report

        for size, size_data in results.items():
            for duration, metrics in size_data.items():
                self.report.add_result(
                    f"loaded_usage_{size}_entries_{duration}s",
                    "INFO",
                    f"Avg Memory: {metrics['avg_mem_usage']:.2f}%, Avg CPU: {metrics['avg_cpu_usage']:.2f}%"
                )
                for container_id, averages in metrics['container_averages'].items():
                    self.report.add_metric(
                        f"loaded_container_{container_id[:12]}_avg_mem_usage_{size}_{duration}s",
                        averages['avg_mem_usage']
                    )
                    self.report.add_metric(
                        f"loaded_container_{container_id[:12]}_avg_cpu_usage_{size}_{duration}s",
                        averages['avg_cpu_usage']
                    )
