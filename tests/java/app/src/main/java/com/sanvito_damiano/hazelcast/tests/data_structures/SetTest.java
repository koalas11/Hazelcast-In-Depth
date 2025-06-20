package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test program for Hazelcast ISet operations
 */
public class SetTest extends AbstractTest {

    private ISet<String> set;

    public SetTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        set = hazelcastInstance.getSet("test-set");
    }

    @Override
    public void reset() {
        set.clear();
        // Add some initial data
        set.add("item1");
        set.add("item2");
        set.add("item3");
        set.add("item4");
        set.add("item5");
    }

    @Override
    public void cleanup() {
        set.destroy();
        set = null;
    }

    public void testBasicSetOperations() {
        System.out.println("\n=== Testing Basic Set Operations ===");
        
        // Test size
        System.out.println("Testing size operation...");
        if (set.size() == 5) {
            System.out.println("✓ Set size is correct: " + set.size());
        } else {
            System.out.println("✗ Set size is incorrect. Expected: 5, Actual: " + set.size());
        }
        
        // Test contains
        System.out.println("Testing contains operation...");
        boolean containsItem = set.contains("item2");
        boolean doesNotContain = set.contains("nonexistent");
        if (containsItem && !doesNotContain) {
            System.out.println("✓ Contains operation works correctly");
        } else {
            System.out.println("✗ Contains operation failed");
        }
        
        // Test add
        System.out.println("Testing add operation...");
        boolean added = set.add("item6");
        if (added && set.size() == 6 && set.contains("item6")) {
            System.out.println("✓ Add operation works correctly");
        } else {
            System.out.println("✗ Add operation failed");
        }
        
        // Test remove
        System.out.println("Testing remove operation...");
        boolean removed = set.remove("item4");
        if (removed && set.size() == 5 && !set.contains("item4")) {
            System.out.println("✓ Remove operation works correctly");
        } else {
            System.out.println("✗ Remove operation failed");
        }
        
        // Test isEmpty
        System.out.println("Testing isEmpty operation...");
        boolean isEmpty = set.isEmpty();
        if (!isEmpty) {
            System.out.println("✓ IsEmpty operation works correctly");
        } else {
            System.out.println("✗ IsEmpty operation failed");
        }
        
        // Test clear
        System.out.println("Testing clear operation...");
        set.clear();
        if (set.isEmpty() && set.size() == 0) {
            System.out.println("✓ Clear operation works correctly");
        } else {
            System.out.println("✗ Clear operation failed");
        }
    }

    public void testSetUniqueElements() {
        System.out.println("\n=== Testing Set Uniqueness ===");
        
        // Test adding duplicate
        System.out.println("Testing uniqueness with duplicate add...");
        int initialSize = set.size();
        boolean added = set.add("item1"); // Already exists
        
        if (!added && set.size() == initialSize) {
            System.out.println("✓ Set correctly rejected duplicate element");
        } else {
            System.out.println("✗ Set failed to maintain uniqueness");
        }
        
        // Test with null value
        System.out.println("Testing add null value...");
        try {
            set.add(null);
            System.out.println("✗ Set should not allow null values");
        } catch (NullPointerException e) {
            System.out.println("✓ Set correctly rejected null value");
        }
    }

    public void testBulkOperations() {
        System.out.println("\n=== Testing Set Bulk Operations ===");
        
        // Test addAll
        System.out.println("Testing addAll operation...");
        Set<String> toAdd = new HashSet<>(Arrays.asList("item3", "item4", "item5"));
        boolean addedAll = set.addAll(toAdd);
        
        if (addedAll && set.size() == 5) {
            System.out.println("✓ AddAll operation works correctly");
        } else {
            System.out.println("✗ AddAll operation failed");
        }
        
        // Test containsAll
        System.out.println("Testing containsAll operation...");
        Set<String> toCheck = new HashSet<>(Arrays.asList("item1", "item2"));
        boolean containsAll = set.containsAll(toCheck);
        
        if (containsAll) {
            System.out.println("✓ ContainsAll operation works correctly");
        } else {
            System.out.println("✗ ContainsAll operation failed");
        }
        
        // Test removeAll
        System.out.println("Testing removeAll operation...");
        Set<String> toRemove = new HashSet<>(Arrays.asList("item1", "item2", "nonexistent"));
        boolean removedAll = set.removeAll(toRemove);
        
        if (removedAll && set.size() == 3 && !set.contains("item1") && !set.contains("item2")) {
            System.out.println("✓ RemoveAll operation works correctly");
        } else {
            System.out.println("✗ RemoveAll operation failed");
        }
        
        // Test retainAll
        System.out.println("Testing retainAll operation...");
        Set<String> toRetain = new HashSet<>(Arrays.asList("item3", "nonexistent"));
        boolean retained = set.retainAll(toRetain);
        
        if (retained && set.size() == 1 && set.contains("item3")) {
            System.out.println("✓ RetainAll operation works correctly");
        } else {
            System.out.println("✗ RetainAll operation failed");
        }
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing Set Listeners ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] itemAdded = new boolean[1];
        
        // Add an item listener
        System.out.println("Adding item listener...");
        set.addItemListener(new ItemListener<String>() {
            @Override
            public void itemAdded(com.hazelcast.collection.ItemEvent<String> itemEvent) {
                if (itemEvent.getItem().equals("new-item")) {
                    itemAdded[0] = true;
                    latch.countDown();
                }
            }
            
            @Override
            public void itemRemoved(com.hazelcast.collection.ItemEvent<String> itemEvent) {
                System.out.println("Item removed: " + itemEvent.getItem());
            }
        }, true);
        
        // Add an item to trigger the listener
        System.out.println("Adding item to trigger listener...");
        set.add("new-item");
        
        // Wait for the listener to be triggered
        boolean received = latch.await(5, TimeUnit.SECONDS);
        
        if (received && itemAdded[0]) {
            System.out.println("✓ Set listener works correctly");
        } else {
            System.out.println("✗ Set listener failed or timed out");
        }
    }

    public void testConcurrentModification() throws Exception {
        System.out.println("\n=== Testing Concurrent Set Modifications ===");
        
        // Reset the set
        set.clear();
        for (int i = 1; i <= 100; i++) {
            set.add("initial-item" + i);
        }
        
        // Number of concurrent threads
        int numThreads = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        
        System.out.println("Starting " + numThreads + " concurrent threads...");
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    // Each thread adds 20 unique items
                    for (int j = 0; j < 20; j++) {
                        set.add("thread" + threadId + "-item" + j);
                    }
                    
                    completeLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        // Start all threads at once
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);
        
        // Verify the results
        int expectedSize = 100 + (numThreads * 20);
        if (completed && set.size() == expectedSize) {
            System.out.println("✓ Concurrent modifications successful. Final size: " + set.size());
        } else {
            System.out.println("✗ Concurrent modifications failed. Expected size: " + expectedSize + 
                              ", Actual size: " + set.size());
        }
    }
}
