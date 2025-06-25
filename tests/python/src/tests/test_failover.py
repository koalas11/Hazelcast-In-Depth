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
            
            for size in [1000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000]:
                logger.info(f"Running failover test with {size} items...")
                
                # Get map and store data
                start = time.time_ns()
                distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
                time_to_retrive_map = time.time_ns() - start
                distributed_map.clear()
                
                # Store test data
                logger.info(f"\tStoring {size} items in the distributed map...")
                data = {f"key{i}": f"value{i}" for i in range(size)}
                distributed_map.put_all(data)
                
                assert distributed_map.size() == size, f"Expected {size} items in the map, found {distributed_map.size()}"
                
                # Kill one node and start measuring recovery time
                logger.info("\tStopping hazelcast-node1...")
                failure_start_time = time.time_ns()
                subprocess.run(["docker", "stop", "hazelcast-node1"], check=True)
                
                map = self.client.get_map("mappa-distribuita-1").blocking()
                map.size()

                time_to_recover = time.time_ns() - failure_start_time - time_to_retrive_map

                logger.info(f"\tNode failure detected, recovery time: {time_to_recover:.2f} nano seconds")
                self.report.add_metric(f"recovery_time_nano_seconds_{size}", time_to_recover)
                self.report.add_result(f"failover_recovery_time_{size}", "PASS", f"Recovery time for {size} items: {time_to_recover:.2f} nano seconds")

                time.sleep(5)
                
                # Restart the node and measure rejoin time
                logger.info("\tRestarting hazelcast-node1...")
                rejoin_start_time = time.time_ns()
                subprocess.run(["docker", "start", "hazelcast-node1"], check=True)

                map = self.client.get_map("mappa-distribuita-1").blocking()
                map.size()

                time_after_rejoin = time.time_ns() - rejoin_start_time - time_to_retrive_map
                
                logger.info(f"\tNode rejoined, rejoin time: {time_after_rejoin:.2f} nano seconds")
                self.report.add_metric(f"rejoin_time_nano_seconds_{size}", time_after_rejoin)
                self.report.add_result(f"failover_rejoin_time_{size}", "PASS", f"Rejoin time for {size} items: {time_after_rejoin:.2f} nano seconds")

                time.sleep(5)

                self.report.add_result(f"failover_test_{size}", "PASS", f"Failover test with {size} items completed successfully")
            return True
        
        except Exception as e:
            logger.error(f"Test failed: {str(e)}")
            self.report.add_result("unexpected_error", "FAIL", f"Error: {str(e)}")
            return False
        