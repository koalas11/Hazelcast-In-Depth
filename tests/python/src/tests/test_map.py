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
            logger.info("Starting timing map test...")
            self.timing()
            logger.info("Timing map test passed successfully.")
            self.report.add_result("timing_map_operations", "PASS", "Timing map operations test passed successfully")

            return True
        except Exception as e:
            logger.error(f"Map test failed: {e}")
            self.report.add_result("timing_map_operations", "FAIL", str(e))
        return False

    def custom_teardown(self):
        distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
        distributed_map.clear()
        distributed_map.destroy()

    def timing(self):
        logger.info("Testing map operations with varying sizes...")
        for size in [1, 10, 100, 1000, 10000]:
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

            self.report.add_metric(f"map_get_time_{size}_ns", get_duration)
            self.report.add_result(f"map_operations_{size}", "PASS", f"Map operations for {size} items completed successfully")

            distributed_map.destroy()  # Clean up the map

            distributed_map = self.client.get_map("mappa-distribuita-1").blocking()

            # Store some test data
            data = {f"key{i}": f"value{i}" for i in range(size)}
            start = time.time_ns()
            distributed_map.put_all(data)
            end = time.time_ns()
            put_all_duration = end - start

            self.report.add_metric(f"map_put_all_time_{size}_ns", put_all_duration)
            self.report.add_result(f"map_put_all_{size}", "PASS", f"Map put_all for {size} items completed successfully")
                
            # Verify data was stored
            keys = [f"key{i}" for i in range(size)]
            start = time.time_ns()
            distributed_map.get_all(keys)
            end = time.time_ns()
            get_all_duration = end - start

            self.report.add_metric(f"map_get_all_time_{size}_ns", get_all_duration)
            self.report.add_result(f"map_get_all_{size}", "PASS", f"Map get_all for {size} items completed successfully")

            distributed_map.destroy()  # Clean up the map
