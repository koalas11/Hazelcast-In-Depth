package com.sanvito_damiano.hazelcast.tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private IMap<RegionAwareKey, String> distributedMap;

    public CustomPartitionTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        // Configure custom partitioning strategy for a specific map
        MapConfig mapConfig = new MapConfig("custom-partitioned-map");
        //mapConfig.setPartitioningStrategyConfig(
        //        new com.hazelcast.config.PartitioningStrategyConfig(
        //                "com.sanvito_damiano.hazelcast.tests.RegionBasedPartitioningStrategy"));
        
        // Configure first member
        Config config1 = new Config();
        config1.setInstanceName("member1");
        config1.setProperty("hazelcast.logging.type", "log4j2");
        config1.addMapConfig(mapConfig);
        
        // Configure second member
        Config config2 = new Config();
        config2.setInstanceName("member2");
        config2.setProperty("hazelcast.logging.type", "log4j2");
        config2.addMapConfig(mapConfig);

        Config config3 = new Config();
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
        hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

        distributedMap = hazelcastInstance.getMap("custom-partitioned-map");
    }

    @Override
    public void reset() {
        distributedMap.clear();
    }

    @Override
    public void cleanup() {
        distributedMap.destroy();
        hazelcastInstance.shutdown();
        memberInstance1.getCluster().shutdown();
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
        System.out.println("\n=== Testing Custom Partitioning Strategy with " + dataSize + " keys per region ===");
        
        // Insert data with regional prefixes
        System.out.println("Inserting region-based data...");
        String[] regions = {"EU", "US", "ASIA", "AF"};
        for (String region : regions) {
            for (int i = 0; i < dataSize; i++) {
                RegionAwareKey regionAwareKey = new RegionAwareKey(region, "key-" + i);
                distributedMap.put(regionAwareKey, "value-" + i);
            }
        }
        
        // Analyze data distribution
        Map<String, Object> customPartitioningResults = analyzeCustomPartitioning(regions, dataSize);

        StringBuilder message = new StringBuilder();
        message.append("Custom partitioning results for ").append(dataSize).append(" keys per region: ");
        message.append("Average distinct partitions per region: ").append(customPartitioningResults.get("avgDistinctPartitions")).append("; ");
        message.append("Region isolation level: ").append(customPartitioningResults.get("regionIsolation")).append(" node overlap; ");
        message.append("Distribution: ").append(customPartitioningResults.get("detailedDistribution"));

        System.out.println("\n" + message.toString());

        recordTestResult("CustomPartitioningTest-with-" + dataSize + "-keys-per-region", true, message.toString());
    }

    /**
     * Analyze how data is distributed using the custom strategy
     */
    private Map<String, Object> analyzeCustomPartitioning(String[] regions, int dataSize) {
        PartitionService partitionService = hazelcastInstance.getPartitionService();
        Map<String, Object> results = new HashMap<>();
        
        System.out.println("Analyzing custom partition distribution by region:");
        
        // Track partitions and keys per node for each region
        Map<String, Map<UUID, Integer>> regionPartitionsPerNode = new HashMap<>();
        Map<String, Map<UUID, Integer>> regionKeysPerNode = new HashMap<>();
        
        // Initialize maps for each region
        for (String region : regions) {
            regionPartitionsPerNode.put(region, new HashMap<>());
            regionKeysPerNode.put(region, new HashMap<>());
        }
        
        StringBuilder detailedResults = new StringBuilder();
        
        for (String region : regions) {
            Map<Integer, Integer> partitionCounts = new HashMap<>();
            Set<Integer> partitionIds = new HashSet<>();
            Map<UUID, Set<Integer>> nodePartitions = new HashMap<>();
            Map<UUID, Integer> nodeKeyCounts = new HashMap<>();
            
            // ✅ CORREZIONE: Usa lo stesso tipo di chiave che hai inserito
            for (int i = 0; i < dataSize; i++) {
                RegionAwareKey regionAwareKey = new RegionAwareKey(region, "key-" + i);
                Partition partition = partitionService.getPartition(regionAwareKey);
                int partitionId = partition.getPartitionId();
                Member owner = partition.getOwner();
                
                if (owner == null) {
                    System.out.println("Partition " + partitionId + " has no owner!");
                    continue;
                }
                
                UUID nodeId = owner.getUuid();
                
                partitionCounts.put(partitionId, partitionCounts.getOrDefault(partitionId, 0) + 1);
                partitionIds.add(partitionId);
                
                // Track partitions per node
                nodePartitions.putIfAbsent(nodeId, new HashSet<>());
                nodePartitions.get(nodeId).add(partitionId);
                
                // Track keys per node
                nodeKeyCounts.put(nodeId, nodeKeyCounts.getOrDefault(nodeId, 0) + 1);
                
                // Print detailed information for the first 3 keys
                if (i < 3) {
                    System.out.println("Key '" + regionAwareKey + "' -> Partition " + partitionId + 
                            " -> Node " + nodeId);
                }
            }
            
            // Store partition and key counts per node for this region
            for (Map.Entry<UUID, Set<Integer>> entry : nodePartitions.entrySet()) {
                regionPartitionsPerNode.get(region).put(entry.getKey(), entry.getValue().size());
            }
            regionKeysPerNode.put(region, nodeKeyCounts);
            
            // Calculate totals for percentages
            int totalPartitionsForRegion = partitionIds.size();
            int totalKeysForRegion = dataSize;
            
            // Print detailed statistics for the region
            System.out.println("\n=== Region " + region + " Distribution ===");
            System.out.println("Total distinct partitions used: " + totalPartitionsForRegion);
            System.out.println("Total keys in region: " + totalKeysForRegion);
            
            detailedResults.append("Region ").append(region).append(": [");
            
            int nodeIndex = 0;
            for (Map.Entry<UUID, Set<Integer>> entry : nodePartitions.entrySet()) {
                UUID nodeId = entry.getKey();
                int nodePartitionCount = entry.getValue().size();
                int nodeKeyCount = nodeKeyCounts.getOrDefault(nodeId, 0);
                
                double partitionPercentage = (double) nodePartitionCount / totalPartitionsForRegion * 100;
                double keyPercentage = (double) nodeKeyCount / totalKeysForRegion * 100;
                
                System.out.println("Node " + nodeIndex + " (" + nodeId + "): " + 
                        nodePartitionCount + " partitions (" + String.format("%.2f", partitionPercentage) + "%), " +
                        nodeKeyCount + " keys (" + String.format("%.2f", keyPercentage) + "%)");
                
                detailedResults.append("Node ").append(nodeIndex).append(": ");
                detailedResults.append(nodePartitionCount).append(" partitions (");
                detailedResults.append(String.format("%.2f", partitionPercentage)).append("% of region partitions), ");
                detailedResults.append(nodeKeyCount).append(" keys (");
                detailedResults.append(String.format("%.2f", keyPercentage)).append("% of region keys)");
                
                if (nodeIndex < nodePartitions.size() - 1) {
                    detailedResults.append("; ");
                }
                nodeIndex++;
            }
            detailedResults.append("]; ");
            
            results.put(region, partitionCounts);
        }
        
        // Calculate overall statistics
        double totalDistinctPartitions = 0;
        for (String region : regions) {
            totalDistinctPartitions += regionPartitionsPerNode.get(region).values().stream()
                    .mapToInt(Integer::intValue).sum();
        }
        
        double avgDistinctPartitions = totalDistinctPartitions / regions.length;
        results.put("avgDistinctPartitions", String.format("%.2f", avgDistinctPartitions));
        
        // Calculate region isolation (overlap between regions)
        System.out.println("\n=== Cross-Region Analysis ===");
        double totalOverlap = 0;
        int comparisons = 0;
        
        for (int i = 0; i < regions.length; i++) {
            for (int j = i + 1; j < regions.length; j++) {
                Set<UUID> region1Nodes = regionPartitionsPerNode.get(regions[i]).keySet();
                Set<UUID> region2Nodes = regionPartitionsPerNode.get(regions[j]).keySet();
                
                Set<UUID> commonNodes = new HashSet<>(region1Nodes);
                commonNodes.retainAll(region2Nodes);
                
                int overlapPercentage = commonNodes.size() * 100 / 
                        Math.max(region1Nodes.size(), region2Nodes.size());
                
                totalOverlap += overlapPercentage;
                comparisons++;
                
                System.out.println("Node overlap between " + regions[i] + " and " + regions[j] + ": " + 
                        commonNodes.size() + " nodes (" + overlapPercentage + "% overlap)");
            }
        }
        
        double regionIsolation = totalOverlap / comparisons;
        results.put("regionIsolation", String.format("%.2f%%", regionIsolation));
        results.put("detailedDistribution", detailedResults.toString());
        
        System.out.println("\nRegion isolation level: " + String.format("%.2f%%", regionIsolation) + 
                " node overlap (lower is better for isolation)");
        
        return results;
    }
}

    
/**
 * Custom partitioning strategy based on region we won't use this in the test
 * but it can be used to handle custom partitioning
 */
class RegionBasedPartitioningStrategy implements PartitioningStrategy<Object> {
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
class RegionAwareKey implements PartitionAware<String> {
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
    
    // ✅ AGGIUNTA: Implementa equals e hashCode
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RegionAwareKey that = (RegionAwareKey) obj;
        return region.equals(that.region) && key.equals(that.key);
    }
    
    @Override
    public int hashCode() {
        return region.hashCode() * 31 + key.hashCode();
    }
    
    @Override
    public String toString() {
        return region + "-" + key;
    }
}
