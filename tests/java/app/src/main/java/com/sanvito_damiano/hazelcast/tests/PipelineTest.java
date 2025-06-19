package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.JoinClause;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.aggregate.AggregateOperations;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Test class to demonstrate Hazelcast Pipeline API functionality
 */
public class PipelineTest extends AbstractTest {

    private IMap<Integer, String> sourceMap;
    private IMap<Integer, String> resultMap;
    private IMap<Integer, String> users;
    private IMap<Integer, Tuple2<Integer, Double>> orders;

    public PipelineTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        sourceMap = hazelcastInstance.getMap("source-map");
        resultMap = hazelcastInstance.getMap("result-map");
        users = hazelcastInstance.getMap("users");
        orders = hazelcastInstance.getMap("orders");
    }

    @Override
    public void reset() {
        sourceMap.clear();
        resultMap.clear();
        users.clear();
        orders.clear();

        // Populate source map with test data
        IMap<Integer, String> sourceMap = hazelcastInstance.getMap("source-map");
        for (int i = 0; i < 100; i++) {
            sourceMap.put(i, "Item-" + i);
        }
        
        // Populate users map
        IMap<Integer, String> users = hazelcastInstance.getMap("users");
        for (int i = 1; i <= 10; i++) {
            users.put(i, "User-" + i);
        }
        
        // Populate orders map
        IMap<Integer, Tuple2<Integer, Double>> orders = hazelcastInstance.getMap("orders");
        for (int i = 1; i <= 30; i++) {
            // User ID between 1-10, Order value between 10-100
            int userId = 1 + (i % 10);
            double value = 10 + (i * 3 % 90);
            orders.put(i, Tuple2.tuple2(userId, value));
        }
    }

    @Override
    public void cleanup() {
        sourceMap.destroy();
        resultMap.destroy();
        users.destroy();
        orders.destroy();

        sourceMap = null;
        resultMap = null;
        users = null;
        orders = null;
    }

    public void testSimplePipeline() {
        System.out.println("\n=== Test 1: Simple Map-Filter-Map Pipeline ===");
        
        Pipeline pipeline = Pipeline.create();
        
        pipeline.readFrom(Sources.<Integer, String>map("source-map"))
                .filter(entry -> entry.getKey() % 2 == 0)  // Only even keys
                .map(entry -> Map.entry(entry.getKey(), "Transformed-" + entry.getValue()))
                .writeTo(Sinks.map("result-map"));
        
        // Execute the pipeline
        Job job = hazelcastInstance.getJet().newJob(pipeline);
        job.join();
        
        // Verify results
        IMap<Integer, String> resultMap = hazelcastInstance.getMap("result-map");
        System.out.println("Result map contains " + resultMap.size() + " entries");
        System.out.println("Sample entries:");
        resultMap.entrySet().stream()
                .limit(5)
                .forEach(entry -> System.out.println(entry.getKey() + " -> " + entry.getValue()));
    }

    public void testAggregationPipeline() {
        System.out.println("\n=== Test 2: Aggregation Pipeline ===");
        
        Pipeline pipeline = Pipeline.create();
        
        pipeline.readFrom(Sources.<Integer, Tuple2<Integer, Double>>map("orders"))
                .groupingKey(entry -> entry.getValue().f0()) // Group by user ID
                .aggregate(AggregateOperations.summingDouble(entry -> entry.getValue().f1()))
                .writeTo(Sinks.map("user-totals"));
        
        // Execute the pipeline
        Job job = hazelcastInstance.getJet().newJob(pipeline);
        job.join();
        
        // Verify results
        IMap<Integer, Double> userTotals = hazelcastInstance.getMap("user-totals");
        System.out.println("User total orders:");
        userTotals.forEach((userId, total) -> 
            System.out.println("User " + userId + " total orders: " + total)
        );
    }

    public void testJoinPipeline() {
        System.out.println("\n=== Test 3: Join Operation Pipeline ===");
        
        Pipeline pipeline = Pipeline.create();
        
        // Join users with their total order values
        BatchStage<Entry<Integer, Double>> userTotals = pipeline.readFrom(Sources.map("user-totals"));
        BatchStage<Entry<Integer, String>> users = pipeline.readFrom(Sources.map("users"));

        BatchStage<Entry<String, Double>> joined = userTotals.hashJoin(
                users,
                JoinClause.onKeys(Entry::getKey, Entry::getKey),
                (orderEntry, userEntry) -> Map.entry(userEntry.getValue(), orderEntry.getValue())
        );

        joined.writeTo(Sinks.map("user-reports"));

        
        // Execute the pipeline
        Job job = hazelcastInstance.getJet().newJob(pipeline);
        job.join();
        
        // Verify results
        IMap<String, Double> userReports = hazelcastInstance.getMap("user-reports");
        System.out.println("User reports after join:");
        userReports.entrySet().stream()
                .forEach(entry -> System.out.println(entry.getKey() + " -> " + entry.getValue().toString()));
    }

    public void testFaultTolerantPipeline() throws InterruptedException {
        System.out.println("\n=== Test 4: Fault Tolerant Pipeline ===");

        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setInstanceName("member3");
        config.setProperty("hazelcast.logging.type", "log4j2");
        HazelcastInstance nodeToShutdown = Hazelcast.newHazelcastInstance(config);
        
        // Create a job config with snapshot capabilities
        JobConfig jobConfig = new JobConfig();
        jobConfig.setProcessingGuarantee(com.hazelcast.jet.config.ProcessingGuarantee.EXACTLY_ONCE);
        jobConfig.setSnapshotIntervalMillis(1000); // Snapshot every second
                
        Pipeline pipeline = Pipeline.create();
                
        pipeline.readFrom(Sources.map("source-map"))
                .filter(entry -> {
                    // Simulate processing delay
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return true;
                })
                .map(entry -> Map.entry(entry.getKey(), "Processed-" + entry.getValue()))
                .writeTo(Sinks.map("ft-result-map"));
        
        // Execute the pipeline with fault tolerance enabled
        Job job = hazelcastInstance.getJet().newJob(pipeline, jobConfig);
        
        // Let it run for a bit
        Thread.sleep(2000);
        
        // Simulate a node failure/restart scenario
        System.out.println("Simulating node shutdown...");
        nodeToShutdown.shutdown();
        
        // In a real test, you would restart the node here
        System.out.println("Job should continue running on remaining nodes");
        
        // Let it finish
        Thread.sleep(3000);
        job.cancel();
        
        // Verify results
        IMap<Integer, String> resultMap = hazelcastInstance.getMap("ft-result-map");
        System.out.println("Result map contains " + resultMap.size() + " entries after fault simulation");
        System.out.println("Sample entries:");
        resultMap.entrySet().stream()
                .limit(5)
                .forEach(entry -> System.out.println(entry.getKey() + " -> " + entry.getValue()));
    }
}
