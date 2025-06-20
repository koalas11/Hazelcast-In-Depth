package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Test program for Hazelcast Data Partitioning, Failover and Custom Partitioning
 */
public class PartitionTest extends AbstractTest {

    public class IntegerPair {
        public Integer f0;
        public Integer f1;

        public IntegerPair(Integer f0, Integer f1) {
            this.f0 = f0;
            this.f1 = f1;
        }
    }

    private static int[] partitionCounts = {100, 500, 1000, 5000, 10000};

    private IMap<String, String> distributedMap;
    
    public PartitionTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        distributedMap = hazelcastInstance.getMap("partitioned-map");
    }

    @Override
    public void reset() {
        distributedMap.clear();
    }
    
    @Override
    public void cleanup() {
        distributedMap.destroy();
        distributedMap = null;
    }

    private void addData(int count) {
        for (int i = 0; i < count; i++) {
            distributedMap.put("key-" + i, "value-" + i);
        }
    }

    public void testPartitionDistribution() throws Exception {
        for (int dataSize : partitionCounts) {
            reset();
            _testPartitionDistributionWithDataLoad(dataSize);
        }
    }
    
    /**
     * Test partition distribution with different data loads
     */
    private void _testPartitionDistributionWithDataLoad(int dataSize) throws Exception {
        System.out.println("\n=== Testing Partition Distribution with " + dataSize + " Data Items ===");
                
        addData(dataSize);
        
        // Analyze partition distribution
        System.out.println("\n=== Partition Distribution for " + dataSize + " Items ===");
        Map<UUID, IntegerPair> partitionStats = analyzePartitionDistribution(dataSize);

        String message = getPartitionChangeMessage(partitionStats, dataSize);

        System.out.println(message);

        recordTestResult("Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);
    }

    /**
     * Test partition distribution with different data loads
     */
    public void testPartitionDistributionAddingNode() throws Exception {
        for (int dataSize : partitionCounts) {
            reset();
            _testAddingNode(dataSize);
        }
    }

    /**
     * Test adding a new node to the cluster
     */
    private void _testAddingNode(int dataSize) throws Exception {
        System.out.println("\n=== Testing Adding Node to Cluster ===");

        addData(dataSize);
        
        // Record initial partition distribution
        System.out.println("Initial partition distribution before adding node:");
        Map<UUID, IntegerPair> initialPartitionStats = analyzePartitionDistribution(dataSize);
        
        String message = getPartitionChangeMessage(initialPartitionStats, dataSize);

        System.out.println(message);

        recordTestResult("AddingNode-Initial-Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);

        // Add a new node to the cluster
        System.out.println("\nAdding new node to cluster...");
        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setInstanceName("member3");
        config.setProperty("hazelcast.logging.type", "log4j2");
        
        HazelcastInstance newNode = Hazelcast.newHazelcastInstance(config);
        
        // Wait for cluster to stabilize
        System.out.println("Waiting for cluster to stabilize after adding new node...");
        Thread.sleep(10000);
        
        // Analyze partition distribution after adding node
        System.out.println("Partition distribution after adding node:");
        Map<UUID, IntegerPair> newPartitionStats = analyzePartitionDistribution(dataSize);

        message = getPartitionChangeMessage(newPartitionStats, dataSize);

        System.out.println(message);

        recordTestResult("AddingNode-New-Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);
        
        // Verify data accessibility
        boolean dataAccessible = verifyDataAccessibility();

        System.out.println("Data accessibility after adding node: " + (dataAccessible ? "Maintained" : "Compromised"));
        recordTestResult("AddingNode-Data-Accessibility-After-Adding-Node-with-" + dataSize + "-Data-Items", dataAccessible, 
                "Data accessibility after adding node: " + (dataAccessible ? "Maintained" : "Compromised"));

        newNode.shutdown();
        
        Thread.sleep(5000); // Allow time for node shutdown
    }

    public void testNodeShutdown() throws Exception {
        for (int dataSize : partitionCounts) {
            reset();
            _testNodeShutdown(dataSize);
        }
    }
    
    /**
     * Test shutting down a node gracefully
     */
    private void _testNodeShutdown(int dataSize) throws Exception {
        System.out.println("\n=== Testing Node Shutdown (Graceful) ===");
        
        addData(dataSize);
        
        // Record initial partition distribution
        System.out.println("Initial partition distribution before node shutdown:");
        Map<UUID, IntegerPair> initialPartitionStats = analyzePartitionDistribution(dataSize);

        String message = getPartitionChangeMessage(initialPartitionStats, dataSize);
        System.out.println(message);
        recordTestResult("NodeShutdown-Initial-Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);
        
        // Add a new node to the cluster
        System.out.println("Adding new node to cluster...");
        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setInstanceName("member3");
        config.setProperty("hazelcast.logging.type", "log4j2");
        
        HazelcastInstance nodeToShutdown = Hazelcast.newHazelcastInstance(config);
        
        // Wait for cluster to stabilize
        System.out.println("Waiting for cluster to stabilize before node shutdown...");
        Thread.sleep(10000);

        // Record the node ID to be shutdown
        UUID nodeId = nodeToShutdown.getCluster().getLocalMember().getUuid();
        System.out.println("Node to shutdown: " + nodeId);

        // Shutdown the node gracefully
        System.out.println("Shutting down node gracefully...");
        nodeToShutdown.shutdown();
        
        // Analyze partition distribution after shutdown
        System.out.println("Partition distribution after node shutdown:");
        Map<UUID, IntegerPair> newPartitionStats = analyzePartitionDistribution(dataSize);

        message = getPartitionChangeMessage(newPartitionStats, dataSize);
        System.out.println(message);
        recordTestResult("NodeShutdown-New-Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);
        
        // Verify data accessibility
        boolean dataAccessible = verifyDataAccessibility();
        System.out.println("Data accessibility after node shutdown: " + (dataAccessible ? "Maintained" : "Compromised"));
        recordTestResult("NodeShutdown-Data-Accessibility-After-Shutdown-with-" + dataSize + "-Data-Items", dataAccessible, 
                "Data accessibility after node shutdown: " + (dataAccessible ? "Maintained" : "Compromised"));
    }
    
    public void testNodeTermination() throws Exception {
        for (int dataSize : partitionCounts) {
            reset();
            _testNodeTermination(dataSize);
        }
    }

    /**
     * Test node termination (simulated crash)
     */
    private void _testNodeTermination(int dataSize) throws Exception {
        System.out.println("\n=== Testing Node Termination ===");
        
        addData(dataSize);
        
        // Record initial partition distribution
        System.out.println("Initial partition distribution before node termination:");
        Map<UUID, IntegerPair> initialPartitionStats = analyzePartitionDistribution(dataSize);

        String message = getPartitionChangeMessage(initialPartitionStats, dataSize);
        System.out.println(message);
        recordTestResult("NodeTermination-Initial-Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);
        
        // Add a new node to the cluster
        System.out.println("Adding new node to cluster...");
        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setInstanceName("member3");
        config.setProperty("hazelcast.logging.type", "log4j2");
        
        HazelcastInstance nodeToTerminate = Hazelcast.newHazelcastInstance(config);
        
        // Wait for cluster to stabilize
        System.out.println("Waiting for cluster to stabilize before node termination...");
        Thread.sleep(10000);

        // Record the node ID to be terminated
        UUID nodeId = nodeToTerminate.getCluster().getLocalMember().getUuid();
        System.out.println("Node to terminate: " + nodeId);

        // Termination the node gracefully
        System.out.println("Shutting down node gracefully...");
        nodeToTerminate.getLifecycleService().terminate();
        
        // Analyze partition distribution after Termination
        System.out.println("Partition distribution after node termination:");
        Map<UUID, IntegerPair> newPartitionStats = analyzePartitionDistribution(dataSize);

        message = getPartitionChangeMessage(newPartitionStats, dataSize);
        System.out.println(message);
        recordTestResult("NodeTermination-New-Partition-Distribution-with-" + dataSize + "-Data-Items", true, message);
        
        // Verify data accessibility
        boolean dataAccessible = verifyDataAccessibility();
        System.out.println("Data NodeTermination after node termination: " + (dataAccessible ? "Maintained" : "Compromised"));
        recordTestResult("NodeTermination-Data-Accessibility-After-Termination-with-" + dataSize + "-Data-Items", dataAccessible, 
                "Data accessibility after node termination: " + (dataAccessible ? "Maintained" : "Compromised"));
    }
    
    /**
     * Analyze and return the partition distribution among nodes
     */
    private Map<UUID, IntegerPair> analyzePartitionDistribution(int dataSize) {
        PartitionService partitionService = hazelcastInstance.getPartitionService();
        Set<Partition> partitions = partitionService.getPartitions();
        
        // Count partitions per node
        Map<UUID, IntegerPair> partitionsPerNode = new HashMap<>();
        
        for (Partition partition : partitions) {
            Member owner = partition.getOwner();
            if (owner != null) {
                UUID memberId = owner.getUuid();
                partitionsPerNode.putIfAbsent(memberId, new IntegerPair(0, 0));
                IntegerPair current = partitionsPerNode.get(memberId);
                current.f0 += 1; // Increment partition count
            }
        }
        
        // Print statistics
        System.out.println("Total partitions: " + partitions.size());
        System.out.println("Partition distribution across nodes:");
        
        for (Map.Entry<UUID, IntegerPair> entry : partitionsPerNode.entrySet()) {
            System.out.println("Node " + entry.getKey() + ": " + entry.getValue().f0 + " partitions");
        }
        
        // Examine some specific keys
        System.out.println("Partition locations for sample keys:");
        for (int i = 0; i < dataSize; i++) {
            String key = "key-" + i;
            Partition partition = partitionService.getPartition(key);
            Member owner = partition.getOwner();
            if (owner != null) {
                if (i < 5) {
                    System.out.println("Key '" + key + "' -> Partition " + partition.getPartitionId() + 
                                       " -> Node " + owner.getUuid());
                }
                IntegerPair current = partitionsPerNode.get(owner.getUuid());
                if (current == null) {
                    continue; // Skip if no entry for this node
                }
                current.f1 += 1; // Increment partition count
            }
        }
        
        return partitionsPerNode;
    }
    
    /**
     * Verify that all data is still accessible
     */
    private boolean verifyDataAccessibility() {
        System.out.println("\nVerifying data accessibility...");
        boolean allDataAccessible = true;
        int count = distributedMap.size();
        int testedKeys = Math.min(count, 1000); // Test up to 1000 keys
        
        for (int i = 0; i < testedKeys; i++) {
            String key = "key-" + i;
            String value = distributedMap.get(key);
            if (!("value-" + i).equals(value)) {
                allDataAccessible = false;
                System.out.println("Data inconsistency for key: " + key);
                break;
            }
        }
        
        if (allDataAccessible) {
            System.out.println("✓ All data is still accessible and consistent after topology changes");
        } else {
            System.out.println("✗ Data inconsistency detected");
        }
        
        return allDataAccessible;
    }

    
    /**
     * Get partition change message
     */
    private String getPartitionChangeMessage(Map<UUID, IntegerPair> partitionStats, int dataSize) {
        double totalPartitions = partitionStats.values().stream().map(f -> f.f0).mapToInt(Integer::intValue).sum();
        double totalEntries = partitionStats.values().stream().map(f -> f.f1).mapToInt(Integer::intValue).sum();

        StringBuilder message = new StringBuilder();
        message.append("Total Partitions: ").append(partitionStats.size()).append("; ");

        message.append("Partition distribution for ").append(dataSize).append(" items: [");
        int count = 0;
        for (Map.Entry<UUID, IntegerPair> entry : partitionStats.entrySet()) {
            message.append("Node ").append(count++).append(": ");
            message.append(entry.getValue().f0).append(" partitions (");
            message.append(String.format("%.2f", (entry.getValue().f0 / totalPartitions) * 100)).append("% of total partitions");
            message.append("), ");
            message.append("Entries: ").append(entry.getValue().f1);
            message.append(" (").append(String.format("%.2f", (entry.getValue().f1 / totalEntries) * 100)).append("% of total entries)");
            if (count < partitionStats.size()) {
                message.append("; ");
            }
        }
        message.append("]");

        return message.toString();
    }
}
