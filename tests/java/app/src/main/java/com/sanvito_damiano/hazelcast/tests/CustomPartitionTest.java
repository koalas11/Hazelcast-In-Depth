package com.sanvito_damiano.hazelcast.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionAware;
import com.hazelcast.partition.PartitionService;
import com.hazelcast.partition.PartitioningStrategy;

public class CustomPartitionTest extends AbstractTest {

    private static final int[] partitionCounts = {100, 500, 1000, 5000, 10000};

    private HazelcastInstance memberInstance1;
    @SuppressWarnings("unused")
    private HazelcastInstance memberInstance2;
    @SuppressWarnings("unused")
    private HazelcastInstance memberInstance3;
    private HazelcastInstance clientInstance;

    private IMap<String, String> distributedMap;

    public CustomPartitionTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        // Configure custom partitioning strategy for a specific map
        MapConfig mapConfig = new MapConfig("custom-partitioned-map");
        mapConfig.setPartitioningStrategyConfig(
                new com.hazelcast.config.PartitioningStrategyConfig(
                        "com.sanvito_damiano.hazelcast.tests.PartitioningTest"));
        
        // Configure first member
        Config config1 = new Config();
        config1.getJetConfig().setEnabled(true);
        config1.setInstanceName("member1");
        config1.setProperty("hazelcast.logging.type", "log4j2");
        config1.addMapConfig(mapConfig);
        
        // Configure second member
        Config config2 = new Config();
        config2.getJetConfig().setEnabled(true);
        config2.setInstanceName("member2");
        config2.setProperty("hazelcast.logging.type", "log4j2");
        config2.addMapConfig(mapConfig);

        Config config3 = new Config();
        config3.getJetConfig().setEnabled(true);
        config3.setInstanceName("member3");
        config3.setProperty("hazelcast.logging.type", "log4j2");
        config3.addMapConfig(mapConfig);

        // Configure client
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setInstanceName("client");
        clientConfig.setProperty("hazelcast.logging.type", "log4j2");

        // Start member instances
        memberInstance1 = Hazelcast.newHazelcastInstance(config1);
        memberInstance2 = Hazelcast.newHazelcastInstance(config2);
        memberInstance3 = Hazelcast.newHazelcastInstance(config3);
        clientInstance = HazelcastClient.newHazelcastClient(clientConfig);

        distributedMap = clientInstance.getMap("custom-partitioned-map");
    }

    @Override
    public void reset() {
        distributedMap.clear();
    }

    @Override
    public void cleanup() {
        distributedMap.destroy();
        clientInstance.shutdown();
        memberInstance1.getCluster().shutdown();
    }

    private void addData(int count) {
        for (int i = 0; i < count; i++) {
            distributedMap.put("key-" + i, "value-" + i);
        }
    }

    public void testCustomPartitioning() throws Exception {
        for (int dataSize : partitionCounts) {
            reset();
            _testCustomPartitioning(dataSize);
        }
    }
    
    /**
     * Test custom partitioning strategy
     */
    private void _testCustomPartitioning(int dataSize) throws Exception {
        System.out.println("\n=== Testing Custom Partitioning Strategy ===");

        addData(dataSize);
        
        // Insert data with regional prefixes
        System.out.println("Inserting region-based data...");
        // Insert 100 elements for each region
        String[] regions = {"EU", "US", "ASIA", "AF"};
        for (String region : regions) {
            for (int i = 0; i < 100; i++) {
                distributedMap.put(region + "-key-" + i, "value-" + i);
            }
        }
        
        // Analyze data distribution
        Map<String, Object> customPartitioningResults = analyzeCustomPartitioning(regions);

        StringBuilder message = new StringBuilder();
        message.append("Custom partitioning results: { ");
        message.append("Average distinct partitions per region: ").append(customPartitioningResults.get("avgDistinctPartitions")).append(", ");
        message.append("Region isolation level: ").append(customPartitioningResults.get("regionIsolation")).append(", ");
        message.append("Detailed partition distribution: [");
        for (String region : regions) {
            message.append("Region: ").append(region).append(", ");
            Object partitions = customPartitioningResults.get(region);
            message.append("Partitions used: ").append(partitions).append("; ");
        }
        message.append("]");

        message.append(" }");
        System.out.println(message.toString());

        recordTestResult("CustomPartitioningTest", true, message.toString());
    }

    /**
     * Analyze how data is distributed using the custom strategy
     */
    private Map<String, Object> analyzeCustomPartitioning(String[] regions) {
        PartitionService partitionService = clientInstance.getPartitionService();
        Map<String, Object> results = new HashMap<>();
        
        System.out.println("\nAnalyzing custom partition distribution by region:");
        
        // For each region, count in which partitions the keys end up
        Map<String, Set<Integer>> regionPartitions = new HashMap<>();
        double totalDistinctPartitions = 0;
        
        for (String region : regions) {
            Map<Integer, Integer> partitionCounts = new HashMap<>();
            Set<Integer> partitionIds = new java.util.HashSet<>();
            
            // Examine keys for each region
            for (int i = 0; i < 50; i++) {
                String key = region + "-key-" + i;
                Partition partition = partitionService.getPartition(key);
                int partitionId = partition.getPartitionId();
                
                partitionCounts.put(partitionId, partitionCounts.getOrDefault(partitionId, 0) + 1);
                partitionIds.add(partitionId);
                
                // Print detailed information for the first 3 keys
                if (i < 3) {
                    Member owner = partition.getOwner();
                    System.out.println("Key '" + key + "' -> Partition " + partitionId + 
                            " -> Node " + (owner != null ? owner.getUuid() : "unknown"));
                }
            }
            
            regionPartitions.put(region, partitionIds);
            totalDistinctPartitions += partitionIds.size();
            
            // Print statistics for the region
            System.out.println("\nRegion " + region + " keys distribution:");
            System.out.println("Number of distinct partitions used: " + partitionIds.size());
            System.out.println("Partition distribution: " + partitionCounts);
        }
        
        // Calculate average distinct partitions per region
        double avgDistinctPartitions = totalDistinctPartitions / regions.length;
        results.put("avgDistinctPartitions", String.format("%.2f", avgDistinctPartitions));
        
        // Verify distinct partition patterns between regions
        System.out.println("\nVerifying distinct partition patterns between regions...");
        double totalOverlap = 0;
        int comparisons = 0;
        
        for (int i = 0; i < regions.length; i++) {
            for (int j = i + 1; j < regions.length; j++) {
                Set<Integer> commonPartitions = new java.util.HashSet<>(regionPartitions.get(regions[i]));
                commonPartitions.retainAll(regionPartitions.get(regions[j]));
                
                int overlapPercentage = commonPartitions.size() * 100 / 
                        Math.max(regionPartitions.get(regions[i]).size(), regionPartitions.get(regions[j]).size());
                
                totalOverlap += overlapPercentage;
                comparisons++;
                
                System.out.println("Common partitions between " + regions[i] + " and " + regions[j] + ": " + 
                        commonPartitions.size() + " (" + overlapPercentage + "% overlap)");
            }
        }
        
        // Calculate region isolation level (lower is better)
        double regionIsolation = totalOverlap / comparisons;
        results.put("regionIsolation", String.format("%.2f%%", regionIsolation));
        
        System.out.println("\nRegion isolation level: " + String.format("%.2f%%", regionIsolation) + 
                " overlap (lower is better)");
        
        return results;
    }
    
    /**
     * Custom partitioning strategy based on region
     */
    public static class RegionBasedPartitioningStrategy implements PartitioningStrategy<Object> {
        @Override
        public Object getPartitionKey(Object key) {
            if (key instanceof String) {
                String stringKey = (String) key;
                // If the key starts with a regional prefix, use the prefix for partitioning
                if (stringKey.startsWith("EU-") || 
                    stringKey.startsWith("US-") || 
                    stringKey.startsWith("ASIA-") || 
                    stringKey.startsWith("AF-")) {
                    return stringKey.substring(0, stringKey.indexOf('-'));
                }
            }
            // Otherwise use the key itself
            return key;
        }
    }
    
    /**
     * Class to test partitioning through PartitionAware
     */
    public static class RegionAwareKey implements PartitionAware<String> {
        private final String region;
        private final String key;
        
        public RegionAwareKey(String region, String key) {
            this.region = region;
            this.key = key;
        }
        
        @Override
        public String getPartitionKey() {
            return region; // Partition by region
        }
        
        @Override
        public String toString() {
            return region + "-" + key;
        }
    }
}
