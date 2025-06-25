package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.map.IMap;
import com.hazelcast.cluster.Member;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Test class to demonstrate Hazelcast Executor Service functionality
 */
public class ExecutorServiceTest extends AbstractTest {

    private IExecutorService executor;

    public ExecutorServiceTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        executor = hazelcastInstance.getExecutorService("test-executor");
    }

    @Override
    public void reset() {
    }

    @Override
    public void cleanup() {
        executor.shutdownNow();
        executor = null;
        IMap<String, Integer> map = hazelcastInstance.getMap("map");
        map.destroy(); // Clean up the distributed map used in tests
    }

    // Test 1: Submit a simple task to a specific member
    public void testExecutorService() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Test Execute on Specific Member ===");
        Member targetMember = hazelcastInstance.getCluster().getMembers().iterator().next(); // Get any member from the cluster
        Future<String> future = executor.submitToMember(new SimpleTask("Test 1"), targetMember);
        System.out.println("Result from specific member: " + future.get());
        recordTestResult("Executor Service Test", true, "All tests executed successfully.");
    }

    // Test 2: Submit a task to all members
    public void testSubmitToAllMembers() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Test Execute on All Members ===");
        Map<Member, Future<String>> allResults = executor.submitToAllMembers(new SimpleTask("Test 2"));
        
        for (Map.Entry<Member, Future<String>> entry : allResults.entrySet()) {
            Member member = entry.getKey();
            Future<String> result = entry.getValue();
            System.out.println("Member " + member.getAddress() + ": " + result.get());
        }
        recordTestResult("Submit to All Members", true, "All members executed the task successfully.");
    }

    // Test 3: Execute task based on key ownership
    public void testExecuteOnKeyOwner() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Test Execute on Key Owner ===");
        String key = "test-key";
        Future<String> keyOwnerFuture = executor.submitToKeyOwner(new KeyAwareTask(key), key);
        System.out.println("Result from key owner: " + keyOwnerFuture.get());
        recordTestResult("Execute on Key Owner", true, "Task executed on key owner successfully.");
    }

    // Test 4: Execute task with multiple parameters
    public void testExecuteWithMultipleParameters() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Test Execute with Multiple Parameters ===");
        Future<Integer> calculationFuture = executor.submit(new CalculationTask(5, 10));
        Integer result = calculationFuture.get();
        System.out.println("Calculation result: " + result);
        recordTestResult("Execute with Multiple Parameters", true, "Calculation executed successfully with result: " + result);
    }

    // Test 5: Execute a task that sums values from a distributed map
    public void testSumTask() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Test Sum Task ===");
        
        // Create a distributed map and populate it with test data
        IMap<String, Integer> map = hazelcastInstance.getMap("map");
        for (int i = 1; i <= 10; i++) {
            map.put("key-" + i, i * 10); // Values are multiples of 10
        }
        
        Future<Integer> sumFuture = executor.submit(new SumTask());
        Integer sumResult = sumFuture.get();
        
        System.out.println("Sum of values in the map: " + sumResult);
        
        // Verify result
        int expectedSum = 10 + 20 + 30 + 40 + 50 + 60 + 70 + 80 + 90 + 100; // Sum of multiples of 10
        boolean testPassed = (sumResult == expectedSum);
        
        recordTestResult("Sum Task", testPassed, 
            "Expected sum: " + expectedSum + ", Actual sum: " + sumResult);
        map.destroy();
    }

    // Test 6: Local data processing task
    public void testLocalDataProcessing() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Test Local Data Processing Task ===");
        
        // Create a distributed map and populate it with test data
        IMap<String, Integer> map = hazelcastInstance.getMap("map");
        for (int i = 1; i <= 100; i++) {
            map.put("key-" + i, i); // Values are just the keys
        }
        
        Map<Member, Future<Integer>> results = executor.submitToAllMembers(new LocalDataProcessingTask());
        
        int totalSum = 0;
        for (Map.Entry<Member, Future<Integer>> entry : results.entrySet()) {
            Member member = entry.getKey();
            Integer partialSum = entry.getValue().get();
            totalSum += partialSum;
            System.out.println("Member " + member.getAddress() + " processed local data and returned: " + partialSum);
        }
        
        System.out.println("Total sum across all members: " + totalSum);
        
        // Verify result
        int expectedSum = (100 * 101) / 2; // Sum of numbers from 1 to 100
        boolean testPassed = (totalSum == expectedSum);
        
        recordTestResult("Local Data Processing Task", testPassed, 
            "Expected sum: " + expectedSum + ", Actual sum: " + totalSum);
        map.destroy();
    }
    
    // Task classes
    
    /**
     * A simple task that returns information about where it's running
     */
    static class SimpleTask implements Callable<String>, Serializable, HazelcastInstanceAware {
        private transient HazelcastInstance hazelcastInstance;

        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hazelcastInstance = hazelcastInstance;
        }

        private final String taskName;
        
        public SimpleTask(String taskName) {
            this.taskName = taskName;
        }
        
        @Override
        public String call() {
            return taskName + " executed on " + hazelcastInstance.getCluster().getLocalMember();
        }
    }
    
    /**
     * A task that's aware of the key it's processing
     */
    static class KeyAwareTask implements Callable<String>, Serializable, HazelcastInstanceAware {
        private transient HazelcastInstance hazelcastInstance;

        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hazelcastInstance = hazelcastInstance;
        }

        private final String key;
        
        public KeyAwareTask(String key) {
            this.key = key;
        }
        
        @Override
        public String call() {
            Member localMember = hazelcastInstance.getCluster().getLocalMember();
            return "Processing key '" + key + "' on member " + localMember.getAddress();
        }
    }
    
    /**
     * A task that performs a calculation
     */
    static class CalculationTask implements Callable<Integer>, Serializable {
        private final int a;
        private final int b;
        
        public CalculationTask(int a, int b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public Integer call() {
            // Simulate complex calculation
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return a + b;
        }
    }

    /**
     * Task that sums values from a distributed map
     */
    static class SumTask implements Callable<Integer>, Serializable, HazelcastInstanceAware {

        private transient HazelcastInstance hazelcastInstance;

        public void setHazelcastInstance( HazelcastInstance hazelcastInstance ) {
            this.hazelcastInstance = hazelcastInstance;
        }

        public Integer call() throws Exception {
            IMap<String, Integer> map = hazelcastInstance.getMap( "map" );
            int result = 0;
            for ( String key : map.localKeySet() ) {
                System.out.println( "Calculating for key: " + key );
                result += map.get( key );
            }
            System.out.println( "Local Result: " + result );
            return result;
        }
    }

    /**
     * Task that processes data locally on each member to optimize performance
     */
    static class LocalDataProcessingTask implements Callable<Integer>, Serializable, HazelcastInstanceAware {
        private transient HazelcastInstance hazelcastInstance;
        
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hazelcastInstance = hazelcastInstance;
        }
        
        @Override
        public Integer call() {
            IMap<String, Integer> map = hazelcastInstance.getMap("map");
            Member localMember = hazelcastInstance.getCluster().getLocalMember();
            
            int sum = 0;
            int processedItems = 0;
            
            // Only process data that's local to this member for optimal performance
            for (String key : map.localKeySet()) {
                Integer value = map.get(key);
                sum += value;
                processedItems++;
            }
            
            System.out.println("Member " + localMember.getAddress() + " processed " + 
                              processedItems + " local entries");
            
            return sum;
        }
    }
}
