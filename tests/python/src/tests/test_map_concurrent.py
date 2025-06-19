import logging
import threading
import time

import hazelcast

from client import create_client

from .test import Test


class MapConcurrentTest(Test):
    """Test class for verifying failover capabilities in Hazelcast cluster"""
    
    logger: logging.Logger
    client2: hazelcast.HazelcastClient

    def __init__(self, test_info: str):
        super().__init__("map_test", test_info)
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
            self.basic_map_operations()

            return True
        except Exception as e:
            self.logger.error(f"Map test failed: {e}")
            self.report.add_result("map_operations", "FAIL", str(e))
        return False
    
    def basic_map_operations(self):
        self.logger.info("Starting concurrent map operations test...")
        
        # Create a shared map that both clients will access
        map_name = "concurrent-map-test"
        distributed_map1 = self.client.get_map(map_name).blocking()
        distributed_map2 = self.client2.get_map(map_name).blocking()
        
        # Thread functions for concurrent operations
        def client1_operations():
            self.logger.info("Client 1 starting operations")
            for i in range(100):
                assert distributed_map1.put_if_absent(f"key{i}", f"value-client1-{i}") in [None, f"value-client2-{i}"]
                time.sleep(0.01)  # Small delay to increase chance of concurrency
            self.logger.info("Client 1 finished operations")
            
        def client2_operations():
            self.logger.info("Client 2 starting operations")
            for i in range(50, 150):  # Overlapping range with client1
                assert distributed_map2.put_if_absent(f"key{i}", f"value-client2-{i}") in [None, f"value-client1-{i}"]
                time.sleep(0.01)  # Small delay to increase chance of concurrency
            self.logger.info("Client 2 finished operations")
        
        # Start both operations concurrently
        thread1 = threading.Thread(target=client1_operations)
        thread2 = threading.Thread(target=client2_operations)
        
        thread1.start()
        thread2.start()
        
        # Wait for both to complete
        thread1.join()
        thread2.join()
        
        # Verify results
        self.logger.info("Verifying concurrent operations results...")
        map_size = distributed_map1.size()
        self.logger.info(f"Final map size: {map_size}")
        
        # We expect 150 keys (0-149)
        assert map_size == 150, f"Expected 150 keys but found {map_size}"
        
        # Check which client's operations took precedence in the overlap region
        overlap_results = {"client1": 0, "client2": 0}
        for i in range(50, 100):
            value = distributed_map1.get(f"key{i}")
            if value == f"value-client1-{i}":
                overlap_results["client1"] += 1
            elif value == f"value-client2-{i}":
                overlap_results["client2"] += 1
            else:
                self.logger.warning(f"Unexpected value for key{i}: {value}")
        
        self.logger.info(f"In overlap region (50-99): Client1 won {overlap_results['client1']} times, Client2 won {overlap_results['client2']} times")
        
        # Also verify that client2 can see changes from client1 and vice versa
        for i in range(0, 50):
            assert distributed_map2.get(f"key{i}") == f"value-client1-{i}", f"Client2 cannot see Client1's changes for key{i}"
            
        for i in range(100, 150):
            assert distributed_map1.get(f"key{i}") == f"value-client2-{i}", f"Client1 cannot see Client2's changes for key{i}"
            
        self.logger.info("Concurrent map operations test completed successfully")
        self.report.add_result("concurrent_map_operations", "PASS", "Concurrent operations completed with proper consistency")
        
        # Clean up
        distributed_map1.destroy()
        distributed_map2.destroy()
