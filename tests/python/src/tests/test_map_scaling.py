import logging
import time
import traceback
from concurrent.futures import Future, ThreadPoolExecutor

import hazelcast
from ..client import create_client
from .test import Test


class MapScalingTest(Test):
    """Test class for verifying failover capabilities in Hazelcast cluster"""
    
    logger: logging.Logger
    client2: hazelcast.HazelcastClient

    def __init__(self, test_info: str):
        super().__init__("map_scaling_test", test_info)
        self.logger = logging.getLogger(__name__)

    def setup(self):
        """Setup method to initialize the Hazelcast client"""
        self.client = create_client(["127.0.0.1:5701"])
        self.client2 = create_client(["127.0.0.1:5702"])

    def custom_teardown(self):
        """Custom teardown logic for specific tests"""
        if self.client2:
            self.client2.shutdown()

    def run_test(self) -> bool:
        """Test basic map operations"""
        try:
            self.logger.info("Starting base map concurrent operations test...")
            self.map_scaling_test()
            self.logger.info("Map scaling test completed successfully.")
            self.report.add_result("map_scaling_operations", "PASS", "Map scaling operations test passed successfully")
            return True
        except Exception as e:
            self.logger.error(f"Map test failed: {e}\n{traceback.format_exc()}")
            self.report.add_result("map_operations", "FAIL", str(e))
        return False
    
    def map_scaling_test(self):
        """Test how Hazelcast map scales with increasing load and concurrent operations"""
        map_name = "scaling-test-map"
        batch_sizes = [1000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000]
        thread_counts = [2, 4, 8, 16]
        
        self.logger.info("Starting map scaling test...")
        
        def insert_data(client: hazelcast.HazelcastClient, start_idx, count, results, thread_id):
            try:
                dist_map = client.get_map(map_name).blocking()
                start_time = time.time_ns()
                
                for i in range(start_idx, start_idx + count):
                    key = f"key_{i}"
                    value = f"value_{i}_{thread_id}"
                    dist_map.put(key, value)
                    
                elapsed = time.time_ns() - start_time
                ops_per_second = count / elapsed if elapsed > 0 else 0
                results[thread_id] = (count, elapsed, ops_per_second)
            except Exception as e:
                self.logger.error(f"Thread {thread_id} failed: {e}\n{traceback.format_exc()}")
                results[thread_id] = (0, 0, 0)

        def get_data(client: hazelcast.HazelcastClient, start_idx, count, results, thread_id):
            try:
                dist_map = client.get_map(map_name).blocking()
                start_time = time.time_ns()
                
                for i in range(start_idx, start_idx + count):
                    key = f"key_{i}"
                    dist_map.get(key)
                    
                elapsed = time.time_ns() - start_time
                ops_per_second = count / elapsed if elapsed > 0 else 0
                results[thread_id] = (count, elapsed, ops_per_second)
            except Exception as e:
                self.logger.error(f"Thread {thread_id} failed: {e}\n{traceback.format_exc()}")
                results[thread_id] = (0, 0, 0)
        
        for batch_size in batch_sizes:
            for thread_count in thread_counts:
                test_map = self.client.get_map(map_name).blocking()
                test_map.clear()
                
                items_per_thread = batch_size // thread_count
                results = {}
                
                self.logger.info(f"Testing with {batch_size} items across {thread_count} threads")
                
                start_time = time.time_ns()

                with ThreadPoolExecutor(max_workers=thread_count) as executor:
                    # Inserimento dati in parallelo
                    futures: list[Future] = []
                    for t in range(thread_count):
                        client_to_use = self.client if t % 2 == 0 else self.client2
                        start_idx = t * items_per_thread
                        futures.append(executor.submit(insert_data, client_to_use, start_idx, items_per_thread, results, t))

                    for future in futures:
                        future.result()

                    # Recupero dati in parallelo
                    futures.clear()
                    for t in range(thread_count):
                        client_to_use = self.client if t % 2 == 0 else self.client2
                        start_idx = t * items_per_thread
                        futures.append(executor.submit(get_data, client_to_use, start_idx, items_per_thread, results, t))
                    
                    for future in futures:
                        future.result()
                    
                total_elapsed = time.time_ns() - start_time
                total_ops = sum(r[0] for r in results.values())
                total_ops_per_second = total_ops / total_elapsed if total_elapsed > 0 else 0
                
                actual_size = test_map.size()
                
                self.logger.info(f"Results: {batch_size} items, {thread_count} threads")
                self.logger.info(f"Total time (ns): {total_elapsed}s")
                self.logger.info(f"Operations per nano second: {total_ops_per_second:.2f}")
                self.logger.info(f"Map size: {actual_size}")

                self.report.add_metric(f"total_time_{batch_size}_{thread_count}", total_elapsed)
                self.report.add_metric(f"ops_per_nano_second_{batch_size}_{thread_count}", total_ops_per_second)
                
                result = "PASS" if actual_size == batch_size else "FAIL"
                self.report.add_result(
                    f"map_scaling_{batch_size}_{thread_count}",
                    result,
                    f"Ops/nano_sec: {total_ops_per_second:.2f}"
                )

                time.sleep(0.5)
