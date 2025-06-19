package com.sanvito_damiano.hazelcast;

import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.tests.*;
import com.sanvito_damiano.hazelcast.tests.data_structures.*;

public class Main {
    private static Map<String, Class<? extends AbstractTest>> tests = new LinkedHashMap<>() {{
        put("map", MapTest.class);
        put("multi_map", MultiMapTest.class);
        put("replicated_map", ReplicatedMapTest.class);
        put("queue", QueueTest.class);
        put("set", SetTest.class);
        put("list", ListTest.class);
        put("topic", TopicTest.class);
        put("ringbuffer", RingBufferTest.class);
        put("atomic_long", AtomicLongTest.class);
        put("executor", ExecutorServiceTest.class);
        put("pipeline", PipelineTest.class);
        put("partition", PartitionTest.class);
    }};

    private static Map<String, Class<? extends AbstractTest>> special_tests = new LinkedHashMap<>() {{
        put("custom_partition", PartitionTest.class);
    }};

    private static HazelcastInstance memberInstance1;
    @SuppressWarnings("unused")
    private static HazelcastInstance memberInstance2;
    private static HazelcastInstance clientInstance;

    public static void main(String[] args) {
        String folder;
        if (args.length > 0) {
            folder = args[0];
            if (!folder.matches("[a-zA-Z0-9_\\-]+")) {
                System.err.println("Invalid folder name. Only alphanumeric characters, underscores, and hyphens are allowed.");
                return;
            }
        } else {
            folder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        System.setProperty("LOG_DIR", folder);

        try {
            new File(folder, "logs").mkdirs(); // Ensure logs directory exists

            // Configure first member
            Config config1 = new Config();
            config1.getJetConfig().setEnabled(true);
            config1.setInstanceName("member1");
            config1.setProperty("hazelcast.logging.type", "log4j2");
            
            // Configure second member
            Config config2 = new Config();
            config2.getJetConfig().setEnabled(true);
            config2.setInstanceName("member2");
            config2.setProperty("hazelcast.logging.type", "log4j2");

            // Configure client
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.setInstanceName("client");
            clientConfig.setProperty("hazelcast.logging.type", "log4j2");

            // Create a Hazelcast cluster with two instances
            memberInstance1 = Hazelcast.newHazelcastInstance(config1);
            memberInstance2 = Hazelcast.newHazelcastInstance(config2);

            clientInstance = HazelcastClient.newHazelcastClient(clientConfig);
            
            for (Entry<String, Class<? extends AbstractTest>> testEntry : tests.entrySet()) {
                try {
                    String testName = testEntry.getKey();
                    AbstractTest testInstance = testEntry.getValue().getConstructor(HazelcastInstance.class, String.class).newInstance(clientInstance, testName);

                    System.out.println("Running tests for: " + testName);
                    testInstance.setup();
                    
                    for (Method method : testInstance.getClass().getMethods())
                    {
                        try {
                            testInstance.reset();
                            if (method.getName().startsWith("test")) {
                                System.out.println("Executing: " + method.getName());
                                method.invoke(testInstance);
                            }
                        } catch (Exception e) {
                            System.err.println("Error during test execution for: " + testName + " - " + method.getName());
                            e.printStackTrace();
                            testInstance.recordTestResult(method.getName(), false, "Failed to execute test: " + e.getMessage());
                        }
                    }
                    testInstance.cleanup();
                    
                    String report = testInstance.generateReport(folder, testName);
                    System.out.println("Test report generated: " + report);
                    System.out.println(testInstance.getSummary());
                } catch (Exception e) {
                    System.err.println("Failed to run test: " + testEntry.getKey());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("An Unexpected error happened: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Shutdown the Hazelcast instances
            clientInstance.shutdown();
            memberInstance1.getCluster().shutdown();
        }

        try {
            for (Entry<String, Class<? extends AbstractTest>> testEntry : special_tests.entrySet()) {
                try {
                    String testName = testEntry.getKey();
                    AbstractTest testInstance = testEntry.getValue().getConstructor(HazelcastInstance.class, String.class).newInstance(clientInstance, testName);

                    System.out.println("Running tests for: " + testName);
                    testInstance.setup();
                    
                    for (Method method : testInstance.getClass().getMethods())
                    {
                        try {
                            testInstance.reset();
                            if (method.getName().startsWith("test")) {
                                System.out.println("Executing: " + method.getName());
                                method.invoke(testInstance);
                            }
                        } catch (Exception e) {
                            System.err.println("Error during test execution for: " + testName + " - " + method.getName());
                            e.printStackTrace();
                            testInstance.recordTestResult(method.getName(), false, "Failed to execute test: " + e.getMessage());
                        }
                    }
                    testInstance.cleanup();
                    
                    String report = testInstance.generateReport(folder, testName);
                    System.out.println("Test report generated: " + report);
                    System.out.println(testInstance.getSummary());
                } catch (Exception e) {
                    System.err.println("Failed to run test: " + testEntry.getKey());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("An Unexpected error happened: " + e.getMessage());
            e.printStackTrace();
        } finally {
            HazelcastClient.shutdownAll();
            Hazelcast.shutdownAll();
        }
    }
}
