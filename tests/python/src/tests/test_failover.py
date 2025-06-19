import time
import subprocess
import logging
from uuid import UUID
import hazelcast
from hazelcast.cluster import MemberInfo
from hazelcast.partition import PartitionService
from .test import Test

logger = logging.getLogger(__name__)

class FailoverTest(Test):
    """Test class for verifying failover capabilities in Hazelcast cluster"""

    members: list[MemberInfo]

    partition_service: PartitionService
    """Partition service to manage data distribution across members"""
    
    def __init__(self, test_info: str):
        super().__init__("failover_test", test_info)
    
    def track_data_ownership(self, size: int) -> dict[UUID, int]:
        """Track ownership information for multiple keys"""
        ownership_info = {}
        for member in self.client.cluster_service.get_members():
            ownership_info[member.uuid] = 0
        for i in range(size):
            key = f"key{i}"
            uuid = self.partition_service.get_partition_owner(self.partition_service.get_partition_id(key))
            if uuid not in ownership_info:
                logger.warning(f"Key {key} has no owner in the current cluster state")
                self.report.add_result(
                    "ownership_tracking", 
                    "WARNING", 
                    f"Key {key} has no owner in the current cluster state"
                )
                continue
            ownership_info[uuid] = ownership_info[uuid] + 1
        return ownership_info
    
    def run_test(self):
        """Test node failure and failover capability"""
        try:
            self.partition_service: PartitionService = self.client.partition_service
            self.members = self.client.cluster_service.get_members()
            for size in [100, 1000, 10000]:
                logger.info(f"Running failover test with {size} items...")
                # Get map and store data
                distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
                
                # Store some test data
                logger.info(f"\tStoring {size} items in the distributed map...")
                for i in range(size):
                    distributed_map.put(f"key{i}", f"value{i}")
                    
                assert distributed_map.size() == size, f"Expected {size} items in the map, found {distributed_map.size()}"
                logger.info(f"\tSuccessfully stored {size} items in the distributed map.")
                
                # Track which node owns each key before failure
                logger.info("\tTracking data ownership before node failure...")
                put_ownership = self.track_data_ownership(size)

                # Get map and store data
                distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
                
                # Store some test data
                logger.info(f"\tStoring {size} items with put_all in the distributed map...")
                data = {f"key{i}": f"value{i}" for i in range(size)}
                distributed_map.put_all(data)
                del data
                    
                assert distributed_map.size() == size, f"Expected {size} items in the map, found {distributed_map.size()}"
                logger.info(f"\tSuccessfullyf stored {size} items in the distributed map with put_all.")
                
                # Track which node owns each key before failure
                logger.info("\tTracking data ownership before node failure...")
                put_all_ownership = self.track_data_ownership(size)

                # Calculate different in ownership using put and put_all
                for (uuid, value) in put_all_ownership.items():
                    member = next((m for m in self.members if m.uuid == uuid), None)
                    if not member:
                        logger.warning(f"Member with UUID {uuid} not found in current cluster state")
                        continue
                    member_name = f"hazelcast-node{member.address.port % 10}"
                    logger.info(f"\t\tMember {member_name} owns {put_ownership[uuid]} keys using put")
                    logger.info(f"\t\tMember {member_name} owns {value} keys using put_all")
                    difference = value - put_ownership[uuid]
                    difference_percentage = (difference / put_ownership[uuid] * 100) if put_ownership[uuid] > 0 else 0
                    
                    logger.info(f"\t\tMember {member_name} changed ownership: {difference} keys ({difference_percentage:.2f}%)")
                    
                    self.report.add_result(
                        f"ownership_tracking_put_{size}", 
                        "INFO", 
                        f"Member {member_name} owns {put_ownership[uuid]} keys using put"
                    )

                    self.report.add_result(
                        f"ownership_tracking_put_{size}", 
                        "INFO", 
                        f"Member {member_name} owns {put_ownership[uuid]} keys using put_all"
                    )

                    self.report.add_result(
                        f"ownership_changes_put_vs_put_all_{size}", 
                        "INFO", 
                        f"Member {member_name} changed ownership: {difference} keys ({difference_percentage:.2f}%)"
                    )

                    self.report.add_metric(f"ownership_changes_put_vs_put_all_{size}", difference)
                    self.report.add_metric(
                        f"ownership_changes_put_vs_put_all_percentage_{member_name}_{size}", 
                        difference_percentage
                    )
                
                # Kill one node
                logger.info("\tStopping hazelcast-node1...")
                subprocess.run(["docker", "stop", "hazelcast-node1"], check=True)
                time.sleep(5)  # Wait for failover
                
                # Verify data is still accessible
                keys = [f"key{i}" for i in range(size)]
                logger.info("\tVerifying data accessibility after node failure...")
                values = distributed_map.get_all(keys)
                for i in range(size):
                    assert values[f"key{i}"] == f"value{i}", f"Expected value{i} but got {values[f'key{i}']}"

                del keys, values

                # Track ownership after node failure
                logger.info("\tTracking data ownership after node failure...")
                after_ownership = self.track_data_ownership(size)
                
                # Track members ownership
                for uuid in after_ownership.keys():
                    member = next((m for m in self.members if m.uuid == uuid), None)
                    if not member:
                        logger.warning(f"Member with UUID {uuid} not found in current cluster state")
                        continue
                    member_name = f"hazelcast-node{member.address.port % 10}"
                    logger.info(f"\t\tMember {member_name} owns {after_ownership[uuid]} keys after failure")
                    self.report.add_result(
                        f"ownership_tracking_after_failure_{size}", 
                        "INFO", 
                        f"Member {member_name} owns {after_ownership[uuid]} keys after failure"
                    )

                # Analyze ownership changes
                ownership_changes = {}
                for uuid in after_ownership.keys():
                    ownership_changes[uuid] = {
                        "before": put_all_ownership[uuid],
                        "after": after_ownership[uuid],
                        "changed": after_ownership[uuid] - put_all_ownership[uuid]
                    }
                        
                total_changes = sum(sub_dict["changed"] for sub_dict in ownership_changes.values())
                # Add ownership change information to report
                self.report.add_result(
                    f"ownership_changes_after_failure_{size}", 
                    "INFO", 
                    f"{total_changes} keys changed ownership after node failure"
                )
                
                self.report.add_result(f"after_node_failure_{size}", "PASS", "Data still accessible after node failure")
                
                # Continue with existing test logic...
                # Add more data
                for i in range(size, size + int(size * 0.2)):
                    distributed_map.put(f"key{i}", f"value{i}")
                
                self.report.add_result(f"write_after_failure_{size}", "PASS", "Successfully wrote data after node failure")
                
                # Restart the node
                logger.info("Restarting hazelcast-node2...")
                subprocess.run(["docker", "start", "hazelcast-node1"], check=True)
                time.sleep(10)  # Wait for node to rejoin
                
                # Track ownership after node rejoins
                logger.info("Tracking data ownership after node rejoins...")
                final_ownership = self.track_data_ownership(size + int(size * 0.2))
                
                # Track members ownership
                for (uuid, value) in final_ownership.items():
                    member = next((m for m in self.members if m.uuid == uuid), None)
                    if not member:
                        logger.warning(f"Member with UUID {uuid} not found in current cluster state")
                        continue
                    member_name = f"hazelcast-node{member.address.port % 10}"
                    logger.info(f"Member {member_name} owns {value} keys after rejoin")
                    self.report.add_result(
                        f"ownership_tracking_after_rejoin_{size}", 
                        "INFO", 
                        f"Member {member_name} owns {value} keys after rejoin"
                    )

                # Analyze ownership changes
                ownership_changes = {}
                for (uuid, value) in final_ownership.items():
                    ownership_changes[uuid] = {
                        "before": after_ownership.get(uuid, 0),
                        "after": value,
                        "changed": value - after_ownership.get(uuid, 0)
                    }

                # Add ownership change information to report
                total_changes = sum(sub_dict["changed"] for sub_dict in ownership_changes.values())
                self.report.add_result(
                    f"ownership_tracking_after_rejoin_{size}", 
                    "INFO", 
                    f"{total_changes} keys changed ownership after node rejoin"
                )

                # Verify all data is still accessible
                keys = [f"key{i}" for i in range(size + int(size * 0.2))]
                values = distributed_map.get_all(keys)
                for i in range(size + int(size * 0.2)):
                    assert values[f"key{i}"] == f"value{i}", f"Expected value{i} but got {distributed_map.get(f'key{i}')}"

                del keys, values
                logger.info("\tAll data is accessible after node rejoin.")

                distributed_map.destroy()
                
                self.report.add_result(f"after_node_rejoin_{size}", "PASS", "All data accessible after node rejoined")
            
            return True
        
        except Exception as e:
            logger.error(f"Test failed: {str(e)}")
            self.report.add_result("unexpected_error", "FAIL", f"Error: {str(e)}")
            return False
