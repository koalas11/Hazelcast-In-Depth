import time
import subprocess
import logging
from uuid import UUID
import hazelcast
from hazelcast.cluster import MemberInfo
from hazelcast.partition import PartitionService
from hazelcast.statistics import Statistics
from .test import Test

logger = logging.getLogger(__name__)

class FailoverTest(Test):
    """Test class for verifying failover capabilities and recovery time in Hazelcast cluster"""

    members: list[MemberInfo]

    partition_service: PartitionService
    """Partition service to manage data distribution across members"""
    
    def __init__(self, test_info: str):
        super().__init__("failover_test", test_info)
    
    def get_cluster_state(self) -> dict:
        """Get comprehensive state of the cluster"""
        state = {
            "timestamp": time.time(),
            "members": {
                "count": len(self.client.cluster_service.get_members()),
                "details": []
            },
            "partitions": {
                "total": self.partition_service.get_partition_count(),
                "with_owner": 0,
                "without_owner": 0,
                "owner_distribution": {}
            },
            "client": {
                "connection_count": 0,
                "last_statistics": {}
            },
            "cluster_version": None
        }
        
        # Get detailed member information
        for member in self.client.cluster_service.get_members():
            member_info = {
                "uuid": str(member.uuid),
                "address": str(member.address),
                "version": member.version,
                "lite_member": member.lite_member,
                "attributes": dict(member.attributes) if hasattr(member, 'attributes') else {}
            }
            state["members"]["details"].append(member_info)
            
            # Use the first member's version as cluster version
            if state["cluster_version"] is None:
                state["cluster_version"] = member.version
        
        # Check partition ownership
        owner_distribution = {}
        for i in range(state["partitions"]["total"]):
            owner = self.partition_service.get_partition_owner(i)
            if owner:
                state["partitions"]["with_owner"] += 1
                owner_str = str(owner)
                if owner_str not in owner_distribution:
                    owner_distribution[owner_str] = 0
                owner_distribution[owner_str] += 1
            else:
                state["partitions"]["without_owner"] += 1
        
        state["partitions"]["owner_distribution"] = owner_distribution
        
        # Gather client statistics if available
        try:
            # Try to access client statistics if available
            if hasattr(self.client, 'statistics'):
                stats = self.client.statistics
                if hasattr(stats, 'get_statistics'):
                    state["client"]["last_statistics"] = stats.get_statistics()
            
            # Try to get connection count
            if hasattr(self.client, 'connection_manager'):
                if hasattr(self.client.connection_manager, 'active_connections'):
                    state["client"]["connection_count"] = len(self.client.connection_manager.active_connections)
        except Exception as e:
            logger.warning(f"Error gathering client statistics: {e}")
        
        return state
    
    def analyze_cluster_changes(self, before_state: dict, after_state: dict) -> dict:
        """Analyze changes between two cluster states"""
        changes = {
            "time_delta": after_state["timestamp"] - before_state["timestamp"],
            "member_count_delta": after_state["members"]["count"] - before_state["members"]["count"],
            "partition_ownership_delta": {
                "with_owner": after_state["partitions"]["with_owner"] - before_state["partitions"]["with_owner"],
                "without_owner": after_state["partitions"]["without_owner"] - before_state["partitions"]["without_owner"]
            },
            "partition_balance": {
                "before": self.calculate_partition_balance(before_state["partitions"]["owner_distribution"]),
                "after": self.calculate_partition_balance(after_state["partitions"]["owner_distribution"])
            },
            "new_members": [],
            "removed_members": []
        }
        
        # Identify added/removed members
        before_uuids = {m["uuid"] for m in before_state["members"]["details"]}
        after_uuids = {m["uuid"] for m in after_state["members"]["details"]}
        
        for member in after_state["members"]["details"]:
            if member["uuid"] not in before_uuids:
                changes["new_members"].append(member)
                
        for member in before_state["members"]["details"]:
            if member["uuid"] not in after_uuids:
                changes["removed_members"].append(member)
        
        return changes
    
    def calculate_partition_balance(self, owner_distribution: dict) -> float:
        """Calculate the balance of partition distribution (0-1 where 1 is perfectly balanced)"""
        if not owner_distribution or len(owner_distribution) <= 1:
            return 1.0  # Only one member or no data
            
        counts = list(owner_distribution.values())
        avg = sum(counts) / len(counts)
        max_deviation = max(abs(count - avg) for count in counts)
        
        # Calculate as percentage of balanced (1.0 = perfectly balanced)
        if avg == 0:
            return 1.0
        balance = 1.0 - (max_deviation / avg)
        return max(0.0, min(1.0, balance))  # Clamp between 0 and 1
    
    def is_cluster_stable(self, min_stable_partition_balance: float = 0.7) -> bool:
        """Check if the cluster has stabilized"""
        try:
            state = self.get_cluster_state()
            
            # All partitions must have owners
            if state["partitions"]["without_owner"] > 0:
                logger.info(f"Cluster not stable: {state['partitions']['without_owner']} partitions without owners")
                return False
                
            # Check partition balance
            balance = self.calculate_partition_balance(state["partitions"]["owner_distribution"])
            if balance < min_stable_partition_balance:
                logger.info(f"Cluster not stable: partition balance {balance:.2f} below threshold {min_stable_partition_balance}")
                return False
                
            # Check all members are active
            if state["members"]["count"] < 2:  # Assuming we want at least 2 members
                logger.info(f"Cluster not stable: only {state['members']['count']} members active")
                return False
                
            return True
        except Exception as e:
            logger.warning(f"Error checking cluster stability: {str(e)}")
            return False
    
    def wait_for_cluster_stability(self, timeout_seconds: int = 60, poll_interval: float = 0.5) -> tuple[float, dict, list]:
        """Wait for cluster to stabilize and return the recovery time, final state, and state history
        
        Args:
            timeout_seconds: Maximum time to wait for stability
            poll_interval: Time between stability checks
            
        Returns:
            Tuple of (recovery_time_seconds, final_cluster_state, state_history)
            recovery_time will be -1 if timeout occurred
        """
        start_time = time.time()
        state_history = []
        
        while time.time() - start_time < timeout_seconds:
            current_state = self.get_cluster_state()
            state_history.append(current_state)
            
            if self.is_cluster_stable():
                recovery_time = time.time() - start_time
                logger.info(f"Cluster stabilized after {recovery_time:.2f} seconds with {current_state['members']['count']} members")
                return recovery_time, current_state, state_history
                
            time.sleep(poll_interval)
            
        logger.warning(f"Cluster did not stabilize within {timeout_seconds} seconds")
        final_state = self.get_cluster_state()
        state_history.append(final_state)
        return -1, final_state, state_history
    
    def measure_operation_performance(self, size: int, distributed_map) -> dict:
        """Measure performance of different operations"""
        results = {}
        
        # Measure read latency (individual gets)
        start_time = time.time()
        sample_size = min(10, size)
        for i in range(sample_size):
            distributed_map.get(f"key{i}")
        results["read_latency_ms"] = (time.time() - start_time) * 1000 / sample_size
        
        # Measure batch read throughput
        batch_size = min(100, size)
        keys = [f"key{i}" for i in range(batch_size)]
        start_time = time.time()
        distributed_map.get_all(keys)
        batch_time = time.time() - start_time
        results["batch_read_throughput"] = batch_size / batch_time if batch_time > 0 else 0
        results["batch_read_latency_ms"] = batch_time * 1000
        
        # Measure write latency
        start_time = time.time()
        sample_size = 10
        for i in range(sample_size):
            key = f"perf_test_key_{i}"
            distributed_map.put(key, f"perf_test_value_{i}")
        results["write_latency_ms"] = (time.time() - start_time) * 1000 / sample_size
        
        # Measure multi-operation throughput (combination of gets and puts)
        start_time = time.time()
        ops_count = 20
        for i in range(ops_count // 2):
            distributed_map.get(f"key{i}")
            distributed_map.put(f"perf_test_key_{i+10}", f"perf_test_value_{i+10}")
        mixed_time = time.time() - start_time
        results["mixed_throughput"] = ops_count / mixed_time if mixed_time > 0 else 0
        
        return results
        
    def run_test(self):
        """Test node failure and failover capability with recovery time analysis"""
        try:
            self.partition_service = self.client.partition_service
            self.members = self.client.cluster_service.get_members()
            
            # Log initial cluster state
            initial_state = self.get_cluster_state()
            logger.info(f"Initial cluster state: {initial_state['members']['count']} members, " 
                       f"{initial_state['partitions']['with_owner']} partitions with owners, "
                       f"partition balance: {self.calculate_partition_balance(initial_state['partitions']['owner_distribution']):.2f}")
            
            for size in [100, 1000, 10000]:
                logger.info(f"Running failover test with {size} items...")
                
                # Get map and store data
                distributed_map = self.client.get_map("mappa-distribuita-1").blocking()
                distributed_map.clear()
                
                # Store test data
                logger.info(f"\tStoring {size} items in the distributed map...")
                start_time = time.time()
                data = {f"key{i}": f"value{i}" for i in range(size)}
                distributed_map.put_all(data)
                data_load_time = time.time() - start_time
                logger.info(f"\tLoaded {size} items in {data_load_time:.2f} seconds")
                self.report.add_metric(f"data_load_time_{size}", data_load_time)
                
                assert distributed_map.size() == size, f"Expected {size} items in the map, found {distributed_map.size()}"
                
                # Measure baseline performance
                logger.info("\tMeasuring baseline performance...")
                baseline_performance = self.measure_operation_performance(size, distributed_map)
                for op, value in baseline_performance.items():
                    logger.info(f"\t\tBaseline {op}: {value:.2f}")
                    self.report.add_metric(f"baseline_{op}_{size}", value)
                
                # Get current cluster state before failure
                before_failure_state = self.get_cluster_state()
                logger.info(f"\tCluster state before failure: {before_failure_state['members']['count']} members, "
                           f"partition balance: {self.calculate_partition_balance(before_failure_state['partitions']['owner_distribution']):.2f}")
                
                # Kill one node and start measuring recovery time
                logger.info("\tStopping hazelcast-node1...")
                failure_start_time = time.time()
                subprocess.run(["docker", "stop", "hazelcast-node1"], check=True)
                
                # Immediately check the state (should show the disruption)
                time.sleep(1)  # Brief pause to register the change
                disruption_state = self.get_cluster_state()
                logger.info(f"\tImmediate disruption state: {disruption_state['members']['count']} members, "
                           f"{disruption_state['partitions']['without_owner']} partitions without owners")
                
                # Wait for cluster to stabilize and measure recovery time
                logger.info("\tWaiting for cluster to stabilize after node failure...")
                recovery_time, after_failure_state, recovery_history = self.wait_for_cluster_stability(timeout_seconds=120)
                
                # Analyze what happened during recovery
                if recovery_time > 0:
                    logger.info(f"\tCluster recovered in {recovery_time:.2f} seconds after node failure")
                    
                    # Analyze recovery process
                    recovery_analysis = self.analyze_cluster_changes(before_failure_state, after_failure_state)
                    logger.info(f"\tRecovery analysis: {recovery_analysis['member_count_delta']} member count change, "
                               f"{recovery_analysis['partition_ownership_delta']['with_owner']} partition ownership changes")
                    
                    # Partition balance change
                    balance_before = recovery_analysis['partition_balance']['before']
                    balance_after = recovery_analysis['partition_balance']['after']
                    logger.info(f"\tPartition balance changed from {balance_before:.2f} to {balance_after:.2f}")
                    
                    self.report.add_result(
                        f"recovery_time_{size}", 
                        "INFO", 
                        f"Cluster recovered in {recovery_time:.2f} seconds after node failure"
                    )
                    self.report.add_metric(f"recovery_time_seconds_{size}", recovery_time)
                    self.report.add_metric(f"partition_balance_before_failure_{size}", balance_before)
                    self.report.add_metric(f"partition_balance_after_recovery_{size}", balance_after)
                    
                    # Track recovery phases (if we have enough history points)
                    if len(recovery_history) >= 3:
                        partition_without_owner_trend = [
                            state["partitions"]["without_owner"] for state in recovery_history
                        ]
                        logger.info(f"\tPartition recovery trend: {partition_without_owner_trend}")
                        self.report.add_result(
                            f"recovery_phases_{size}",
                            "INFO",
                            f"Partition recovery trend: {partition_without_owner_trend}"
                        )
                else:
                    logger.warning("\tCluster did not stabilize within the timeout period")
                    self.report.add_result(
                        f"recovery_time_{size}", 
                        "WARNING", 
                        "Cluster did not stabilize within the timeout period"
                    )
                
                # Measure post-failure performance
                logger.info("\tMeasuring performance after node failure...")
                after_failure_performance = self.measure_operation_performance(size, distributed_map)
                for op, value in after_failure_performance.items():
                    logger.info(f"\t\tPost-failure {op}: {value:.2f}")
                    self.report.add_metric(f"after_failure_{op}_{size}", value)
                    
                    # Calculate performance impact
                    if baseline_performance[op] > 0:
                        impact_pct = ((value - baseline_performance[op]) / baseline_performance[op]) * 100
                        logger.info(f"\t\tImpact on {op}: {impact_pct:.2f}%")
                        self.report.add_metric(f"impact_pct_{op}_{size}", impact_pct)
                
                # Verify all data is still accessible
                logger.info("\tVerifying data accessibility after node failure...")
                start_time = time.time()
                keys = [f"key{i}" for i in range(size)]
                values = distributed_map.get_all(keys)
                data_access_time = time.time() - start_time
                
                data_accessible = True
                for i in range(size):
                    if values.get(f"key{i}") != f"value{i}":
                        logger.error(f"\tData inconsistency: Expected value{i} but got {values.get(f'key{i}')}")
                        data_accessible = False
                        break
                
                if data_accessible:
                    logger.info(f"\tAll data successfully accessed in {data_access_time:.2f} seconds after node failure")
                    self.report.add_result(f"data_access_after_failure_{size}", "PASS", "All data accessible after node failure")
                else:
                    logger.error("\tData inconsistency detected after node failure")
                    self.report.add_result(f"data_access_after_failure_{size}", "FAIL", "Data inconsistency detected after node failure")
                
                self.report.add_metric(f"data_access_time_after_failure_{size}", data_access_time)
                
                # Restart the node and measure rejoin time
                logger.info("\tRestarting hazelcast-node1...")
                rejoin_start_time = time.time()
                subprocess.run(["docker", "start", "hazelcast-node1"], check=True)
                
                # Wait for cluster to stabilize after rejoin
                logger.info("\tWaiting for cluster to stabilize after node rejoin...")
                rejoin_time, final_state, rejoin_history = self.wait_for_cluster_stability(timeout_seconds=120)
                
                if rejoin_time > 0:
                    logger.info(f"\tCluster stabilized in {rejoin_time:.2f} seconds after node rejoin")
                    
                    # Analyze rejoin process
                    rejoin_analysis = self.analyze_cluster_changes(after_failure_state, final_state)
                    logger.info(f"\tRejoin analysis: {rejoin_analysis['member_count_delta']} member count change, "
                               f"{rejoin_analysis['partition_ownership_delta']['with_owner']} partition ownership changes")
                    
                    # Track rebalancing after rejoin
                    balance_before_rejoin = self.calculate_partition_balance(after_failure_state["partitions"]["owner_distribution"])
                    balance_after_rejoin = self.calculate_partition_balance(final_state["partitions"]["owner_distribution"])
                    logger.info(f"\tPartition balance changed from {balance_before_rejoin:.2f} to {balance_after_rejoin:.2f} after rejoin")
                    
                    self.report.add_result(
                        f"rejoin_time_{size}", 
                        "INFO", 
                        f"Cluster stabilized in {rejoin_time:.2f} seconds after node rejoin"
                    )
                    self.report.add_metric(f"rejoin_time_seconds_{size}", rejoin_time)
                    self.report.add_metric(f"partition_balance_after_rejoin_{size}", balance_after_rejoin)
                    
                    # Track rejoin phases
                    if len(rejoin_history) >= 3:
                        member_count_trend = [state["members"]["count"] for state in rejoin_history]
                        logger.info(f"\tMember count trend during rejoin: {member_count_trend}")
                        self.report.add_result(
                            f"rejoin_phases_{size}",
                            "INFO",
                            f"Member count trend during rejoin: {member_count_trend}"
                        )
                else:
                    logger.warning("\tCluster did not stabilize within the timeout period after node rejoin")
                    self.report.add_result(
                        f"rejoin_time_{size}", 
                        "WARNING", 
                        "Cluster did not stabilize within the timeout period after node rejoin"
                    )
                
                # Measure post-rejoin performance
                logger.info("\tMeasuring performance after node rejoin...")
                after_rejoin_performance = self.measure_operation_performance(size, distributed_map)
                for op, value in after_rejoin_performance.items():
                    logger.info(f"\t\tPost-rejoin {op}: {value:.2f}")
                    self.report.add_metric(f"after_rejoin_{op}_{size}", value)
                    
                    # Calculate performance recovery
                    if baseline_performance[op] > 0:
                        recovery_pct = ((value - baseline_performance[op]) / baseline_performance[op]) * 100
                        logger.info(f"\t\tPerformance difference from baseline for {op}: {recovery_pct:.2f}%")
                        self.report.add_metric(f"performance_recovery_{op}_{size}", recovery_pct)
                
                # Clean up
                distributed_map.destroy()
                
                # Summary for this size
                self.report.add_result(
                    f"failover_summary_{size}", 
                    "INFO", 
                    f"Failover test with {size} items: recovery time {recovery_time:.2f}s, rejoin time {rejoin_time:.2f}s"
                )
            
            return True
        
        except Exception as e:
            logger.error(f"Test failed: {str(e)}")
            self.report.add_result("unexpected_error", "FAIL", f"Error: {str(e)}")
            return False
        