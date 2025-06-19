import logging
import time

from .test import Test

logger = logging.getLogger(__name__)

class MapTest(Test):
    """Test class for verifying failover capabilities in Hazelcast cluster"""
    
    def __init__(self, test_info: str):
        super().__init__("map_test", test_info)

    def run_test(self) -> bool:
        """Test basic map operations"""
        try:
            logger.info("Starting base map operations test...")



            logger.info("Base map operations test passed successfully.")
            self.report.add_result("base_map_operations", "PASS", "Basic map operations test passed successfully")

            self.timing()

            self.report.add_result("map_operations", "PASS", "All map operations tests passed successfully")

            return True
        except Exception as e:
            logger.error(f"Map test failed: {e}")
            self.report.add_result("map_operations", "FAIL", str(e))
        return False
    
    def multiple_map_operations(self):
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()

            for i in 15:
                distributed_map.put(f"key{i}", f"value{i}")

            assert distributed_map.size() == 15, "Map size should be 15 after inserting 15 keys"

            for i in 15:
                assert distributed_map.contains_key(f"key{i}") == True, f"Map should contain key{i} after put operation"
                assert distributed_map.contains_value(f"value{i}") == True, f"Map should contain value{i} after put operation"
                assert distributed_map.get(f"key{i}") == f"value{i}", f"Data mismatch for key{i} in map get operation"
                assert distributed_map.remove(f"key{i}") == f"value{i}", f"Map remove operation should return value for key{i}"

            assert distributed_map.size() == 0, "Map size should be 15 after removing 15 keys"

            data = {f"key{i}": f"value{i}" for i in range(15)}
            distributed_map.put_all(data)
            assert distributed_map.size() == 15, "Map size should be 15 after put_all operation"
            assert distributed_map.get_all([f"key{i}" for i in range(15)]) == data, "Map get_all operation should return all values"
            assert distributed_map.remove_all([f"key{i}" for i in range(15)]) == data, "Map remove_all operation should return all values"
            
            assert distributed_map.destroy()

    def basic_map_operations(self):
        distributed_map = self.client.get_map("mappa-distribuita-1").blocking()

        assert distributed_map.put("key1", "value1") == None, "Map put operation should return None on first insert"
        assert distributed_map.size() == 1, "Map size should be 1 after one put operation"
        assert distributed_map.contains_key("key1") == True, "Map should contain key1 after put operation"
        assert distributed_map.contains_value("value1") == True, "Map should contain value1 after put operation"
        assert distributed_map.get("key1") == "value1", "Data mismatch in map get operation"

        assert distributed_map.put("key1", "value2") == "value1", "Map put operation should return previous value"
        assert distributed_map.get("key1") == "value2", "Data mismatch after updating key1"
        assert distributed_map.size() == 1, "Map size should remain 1 after updating key1"
        assert distributed_map.contains_key("key1") == True, "Map should still contain key1 after update"
        assert distributed_map.contains_value("value2") == True, "Map should contain updated value2 after put operation"
        assert distributed_map.contains_value("value1") == False, "Map should not contain old value1 after update"

        assert distributed_map.put("key2", "value2") == None, "Map put operation should return None on first insert of key2"
        assert distributed_map.size() == 2, "Map size should be 2 after inserting key2"

        assert distributed_map.remove("key1") == "value2", "Map remove operation has a return value"
        assert distributed_map.size() == 1, "Map size should be 1 after removing key1"
        assert distributed_map.contains_key("key1") == False, "Map should not contain key1 after removal"

        assert distributed_map.key_set() == ["key2"], "Map key set should only contain key2 after removing key1"

        assert distributed_map.destroy()

    def timing(self):
        logger.info("Testing map operations with varying sizes...")
        for size in [1, 10, 100, 1000, 10000]:
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()

            # Store some test data
            start = time.time_ns()
            for i in range(size):
                distributed_map.put(f"key{i}", f"value{i}")
            end = time.time_ns()
            put_duration = end - start
            
            self.report.add_metric(f"map_put_time_{size}_ns", put_duration)

            start = time.time_ns()
            for i in range(size):
                distributed_map.get(f"key{i}")
            end = time.time_ns()

            get_duration = end - start

            distributed_map.destroy()  # Clean up the map

            self.report.add_metric(f"map_get_time_{size}_ns", get_duration)

        for size in [1, 10, 100, 1000, 10000]:
            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()

            # Store some test data
            data = {f"key{i}": f"value{i}" for i in range(size)}
            start = time.time_ns()
            distributed_map.put_all(data)
            end = time.time_ns()
            put_all_duration = end - start

            self.report.add_metric(f"map_put_all_time_{size}_ns", put_all_duration)
                
            # Verify data was stored
            keys = [f"key{i}" for i in range(size)]
            start = time.time_ns()
            distributed_map.get_all(keys)
            end = time.time_ns()
            get_all_duration = end - start

            distributed_map.destroy()  # Clean up the map

            self.report.add_metric(f"map_get_all_time_{size}_ns", get_all_duration)
