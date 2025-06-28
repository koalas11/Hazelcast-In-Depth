package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.Config;
import com.hazelcast.config.SplitBrainProtectionConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.splitbrainprotection.SplitBrainProtectionException;
import com.hazelcast.splitbrainprotection.SplitBrainProtectionOn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SplitBrainProtectionTest extends AbstractTest {

    private static final String MAP_NAME = "protected-map";

    public SplitBrainProtectionTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
    }

    @Override
    public void reset() {
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup() {
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }

    /*
     * Test that operations work normally when split-brain protection is satisfied
     */
    public void testMinimumMembersQuorum() throws Exception {
        System.out.println("\n=== Testing Minimum Members Quorum ===");
        
        List<HazelcastInstance> instances = new ArrayList<>();
        try {
            // Create cluster with 3 nodes and minimum 2 nodes quorum
            Config config = createQuorumConfig("minimum-two-members", 2);
            
            // Start 3 instances
            for (int i = 0; i < 3; i++) {
                instances.add(Hazelcast.newHazelcastInstance(config));
            }
            
            // Use the first instance for operations
            IMap<String, String> protectedMap = instances.get(0).getMap(MAP_NAME);
            
            // With 3 nodes, operations should work normally
            protectedMap.put("key1", "value1");
            String value = protectedMap.get("key1");
            boolean success = "value1".equals(value);
            
            if (success) {
                System.out.println("✓ Operations successful with 3 nodes (required: 2)");
            } else {
                System.out.println("✗ Failed to verify retrieved value");
            }
            
            recordTestResult("MinimumMembersQuorum", success, 
                "Minimum members quorum test completed");
        } finally {
            // Clean up instances
            for (HazelcastInstance instance : instances) {
                instance.shutdown();
            }
        }
    }

    /*
     * Test that operations fail when split-brain protection is not satisfied
     */
    public void testQuorumFailure() throws Exception {
        System.out.println("\n=== Testing Quorum Failure ===");
        
        List<HazelcastInstance> instances = new ArrayList<>();
        boolean success = false;
        
        try {
            // Create cluster with 3 nodes and minimum 3 nodes quorum
            Config config = createQuorumConfig("minimum-three-members", 3);
            
            // Start 3 instances
            for (int i = 0; i < 3; i++) {
                instances.add(Hazelcast.newHazelcastInstance(config));
            }
            
            // Use the first instance for operations
            IMap<String, String> protectedMap = instances.get(0).getMap(MAP_NAME);
            
            // With 3 nodes, operations should work normally
            protectedMap.put("key1", "value1");
            System.out.println("✓ Initial write successful with 3 nodes (required: 3)");
            
            // Shut down one instance to break the quorum
            instances.get(2).shutdown();
            instances.remove(2);
            
            // Give some time for cluster to detect member departure
            TimeUnit.SECONDS.sleep(10);
            
            try {
                // This should fail with SplitBrainProtectionException
                protectedMap.put("key2", "value2");
                System.out.println("✗ Write operation succeeded when it should have failed");
            } catch (SplitBrainProtectionException e) {
                success = true;
                System.out.println("✓ Write operation correctly failed with SplitBrainProtectionException");
            }
            
            recordTestResult("QuorumFailure", success, 
                "Quorum failure scenario test completed");
        } finally {
            // Clean up instances
            for (HazelcastInstance instance : instances) {
                instance.shutdown();
            }
        }
    }

    /*
     * Test read/write operations with different quorum types
     */
    public void testReadWriteQuorum() throws Exception {
        System.out.println("\n=== Testing Read/Write Quorum ===");
        
        List<HazelcastInstance> instances = new ArrayList<>();
        boolean success = true;
        
        try {
            // Create cluster with 3 nodes and read quorum of 2, write quorum of 3
            Config config = new Config();
            config.setProperty("hazelcast.logging.type", "log4j2");
            
            // Read quorum config
            SplitBrainProtectionConfig readQuorumConfig = SplitBrainProtectionConfig.newRecentlyActiveSplitBrainProtectionConfigBuilder("read-quorum", 2, 10000).enabled(true).build();
            readQuorumConfig.setProtectOn(SplitBrainProtectionOn.READ);
            config.addSplitBrainProtectionConfig(readQuorumConfig);
            
            // Write quorum config
            SplitBrainProtectionConfig writeQuorumConfig = SplitBrainProtectionConfig.newRecentlyActiveSplitBrainProtectionConfigBuilder("write-quorum", 3, 10000).enabled(true).build();
            writeQuorumConfig.setProtectOn(SplitBrainProtectionOn.WRITE);
            config.addSplitBrainProtectionConfig(writeQuorumConfig);
            
            // Configure the map to use different quorums for read and write
            MapConfig mapConfigRead = new MapConfig(MAP_NAME + "-read");
            mapConfigRead.setSplitBrainProtectionName("read-quorum");
            config.addMapConfig(mapConfigRead);

            MapConfig mapConfigWrite = new MapConfig(MAP_NAME + "-write");
            mapConfigRead.setSplitBrainProtectionName("write-quorum");
            config.addMapConfig(mapConfigWrite);
            
            // Start 3 instances
            for (int i = 0; i < 3; i++) {
                instances.add(Hazelcast.newHazelcastInstance(config));
            }
            
            // Use the first instance for operations
            IMap<String, String> protectedMapRead = instances.get(0).getMap(MAP_NAME + "-read");
            IMap<String, String> protectedMapWrite = instances.get(0).getMap(MAP_NAME + "-write");
            
            // With 3 nodes, write operations should work
            protectedMapRead.put("key1", "value1");
            protectedMapWrite.put("key1", "value1");

            // Verify initial write
            String value = protectedMapRead.get("key1");
            if ("value1".equals(value)) {
                System.out.println("✓ Initial read successful with 3 nodes (required: 2)");
            } else {
                System.out.println("✗ Initial read returned unexpected value");
                recordTestResult("ReadWriteQuorum", false, 
                    "Initial read returned unexpected value");
                throw new RuntimeException("Initial read returned unexpected value");
            }
            value = protectedMapWrite.get("key1");
            if ("value1".equals(value)) {
                System.out.println("✓ Initial write successful with 3 nodes (required: 3)");
            } else {
                System.out.println("✗ Initial write returned unexpected value");
                recordTestResult("ReadWriteQuorum", false, 
                    "Initial read returned unexpected value");
                throw new RuntimeException("Initial read returned unexpected value");
            }

            System.out.println("✓ Initial write successful with 3 nodes (required: 3)");
            
            // Shut down one instance
            instances.get(2).shutdown();
            instances.remove(2);
            
            // Give some time for cluster to detect member departure
            TimeUnit.SECONDS.sleep(10);
            
            try {
                // Read should work (quorum 2)
                value = protectedMapRead.get("key1");
                if ("value1".equals(value)) {
                    System.out.println("✓ Read operation successful with 2 nodes (required: 2)");
                } else {
                    System.out.println("✗ Read operation returned unexpected value");
                    success = false;
                }
                
                // Write should fail (quorum 3)
                protectedMapWrite.set("key2", "value2");
                System.out.println("✗ Write operation succeeded when it should have failed");
                success = false;
            } catch (SplitBrainProtectionException e) {
                System.out.println("✓ Write operation correctly failed with SplitBrainProtectionException");
            }
            
            recordTestResult("ReadWriteQuorum", success, 
                "Read/Write quorum test completed");
        } finally {
            // Clean up instances
            for (HazelcastInstance instance : instances) {
                instance.shutdown();
            }
        }
    }

    /*
     * Test cluster recovery after split-brain is healed
     */
    public void testSplitBrainRecovery() throws Exception {
        System.out.println("\n=== Testing Split-Brain Recovery ===");
        
        List<HazelcastInstance> instances = new ArrayList<>();
        boolean success = true;
        
        try {
            // Create cluster with quorum of 2
            Config config = createQuorumConfig("recovery-quorum", 2);
            
            // Start 3 instances
            for (int i = 0; i < 3; i++) {
                instances.add(Hazelcast.newHazelcastInstance(config));
            }
            
            // Use the first instance for operations
            IMap<String, String> protectedMap = instances.get(0).getMap(MAP_NAME);
            
            // With 3 nodes, operations should work
            protectedMap.put("key1", "value1");
            System.out.println("✓ Initial write successful with 3 nodes");
            
            // Shut down one instance to simulate partition
            HazelcastInstance removedInstance = instances.get(2);
            removedInstance.shutdown();
            instances.remove(2);
            
            // Give some time for cluster to detect member departure
            TimeUnit.SECONDS.sleep(10);
            
            // Operations should still work with 2 nodes
            protectedMap.put("key2", "value2");
            System.out.println("✓ Write operation successful with 2 nodes (required: 2)");
            
            // Start the instance again to heal the cluster
            instances.add(Hazelcast.newHazelcastInstance(config));
            
            // Give some time for cluster to detect new member
            TimeUnit.SECONDS.sleep(10);
            
            // Operations should work after healing
            protectedMap.put("key3", "value3");
            String value = protectedMap.get("key3");
            
            if ("value3".equals(value)) {
                System.out.println("✓ Operations successful after healing the cluster");
            } else {
                System.out.println("✗ Failed to verify data after healing");
                success = false;
            }
            
            recordTestResult("SplitBrainRecovery", success, 
                "Split-brain recovery test completed");
        } finally {
            // Clean up instances
            for (HazelcastInstance instance : instances) {
                instance.shutdown();
            }
        }
    }

    /*
     * Test custom function for split-brain protection
     */
    public void testCustomSplitBrainProtection() throws Exception {
        System.out.println("\n=== Testing Custom Split-Brain Protection Function ===");
        
        List<HazelcastInstance> instances = new ArrayList<>();
        boolean success = true;
        
        try {
            // Create cluster with custom quorum
            Config config = new Config();
            config.setProperty("hazelcast.logging.type", "log4j2");
            
            // Custom quorum config - member UUID based
            SplitBrainProtectionConfig customQuorumConfig = new SplitBrainProtectionConfig();
            customQuorumConfig.setName("custom-quorum");
            customQuorumConfig.setEnabled(true);
            
            // Define a function that requires the master node to be present
            // Note: In a real scenario, you would set a proper custom function
            // This is simplified for the example
            customQuorumConfig.setFunctionImplementation(members -> {
                if (members.isEmpty()) return false;
                // Consider the first created instance as "master"
                return members.size() >= 1;
            });
            
            config.addSplitBrainProtectionConfig(customQuorumConfig);
            
            // Configure the map to use the custom quorum
            MapConfig mapConfig = new MapConfig(MAP_NAME);
            mapConfig.setSplitBrainProtectionName("custom-quorum");
            config.addMapConfig(mapConfig);
            
            // Start 3 instances
            for (int i = 0; i < 3; i++) {
                instances.add(Hazelcast.newHazelcastInstance(config));
            }
            
            // Use the first instance for operations
            IMap<String, String> protectedMap = instances.get(0).getMap(MAP_NAME);
            
            // Initial operation
            protectedMap.put("key1", "value1");
            System.out.println("✓ Initial operation successful with all nodes");
            
            // Shut down two instances but keep the first one (our "master")
            instances.get(2).shutdown();
            instances.get(1).shutdown();
            instances.remove(2);
            instances.remove(1);
            
            // Give some time for cluster to detect member departures
            TimeUnit.SECONDS.sleep(10);
            
            try {
                // This should still work as our custom function only requires the master
                protectedMap.put("key2", "value2");
                String value = protectedMap.get("key2");
                
                if ("value2".equals(value)) {
                    System.out.println("✓ Operations successful with only master node");
                } else {
                    System.out.println("✗ Failed to verify data with custom quorum");
                    success = false;
                }
            } catch (Exception e) {
                System.out.println("✗ Unexpected exception with custom quorum: " + e.getMessage());
                success = false;
            }
            
            recordTestResult("CustomSplitBrainProtection", success, 
                "Custom split-brain protection test completed");
        } finally {
            // Clean up instances
            for (HazelcastInstance instance : instances) {
                instance.shutdown();
            }
        }
    }

    // Helper method to create a config with quorum
    private Config createQuorumConfig(String quorumName, int minSize) {
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "log4j2");
        
        // Create split-brain protection config
        SplitBrainProtectionConfig splitBrainProtectionConfig = 
            SplitBrainProtectionConfig.newRecentlyActiveSplitBrainProtectionConfigBuilder(quorumName, minSize, 10000)
                .enabled(true)
                .build();
        config.addSplitBrainProtectionConfig(splitBrainProtectionConfig);
        
        // Configure the map to use split-brain protection
        MapConfig mapConfig = new MapConfig(MAP_NAME);
        mapConfig.setSplitBrainProtectionName(quorumName);
        config.addMapConfig(mapConfig);
        
        return config;
    }
}
