package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
        boolean sizeCorrect = set.size() == 5;
        if (sizeCorrect) {
            System.out.println("✓ Set size is correct: " + set.size());
        } else {
            System.out.println("✗ Set size is incorrect. Expected: 5, Actual: " + set.size());
        }
        recordTestResult("BasicSetOps-Size", sizeCorrect, 
                         "Set size check. Expected: 5, Actual: " + set.size());
        
        // Test contains
        System.out.println("Testing contains operation...");
        boolean containsItem = set.contains("item2");
        boolean doesNotContain = set.contains("nonexistent");
        boolean containsWorked = containsItem && !doesNotContain;
        if (containsWorked) {
            System.out.println("✓ Contains operation works correctly");
        } else {
            System.out.println("✗ Contains operation failed");
        }
        recordTestResult("BasicSetOps-Contains", containsWorked, 
                         "Contains operation test. Contains item2: " + containsItem + 
                         ", Contains nonexistent: " + doesNotContain);
        
        // Test add
        System.out.println("Testing add operation...");
        boolean added = set.add("item6");
        boolean addWorked = added && set.size() == 6 && set.contains("item6");
        if (addWorked) {
            System.out.println("✓ Add operation works correctly");
        } else {
            System.out.println("✗ Add operation failed");
        }
        recordTestResult("BasicSetOps-Add", addWorked, 
                         "Add operation test. Added: " + added + 
                         ", New size: " + set.size() + 
                         ", Contains new item: " + set.contains("item6"));
        
        // Test remove
        System.out.println("Testing remove operation...");
        boolean removed = set.remove("item4");
        boolean removeWorked = removed && set.size() == 5 && !set.contains("item4");
        if (removeWorked) {
            System.out.println("✓ Remove operation works correctly");
        } else {
            System.out.println("✗ Remove operation failed");
        }
        recordTestResult("BasicSetOps-Remove", removeWorked, 
                         "Remove operation test. Removed: " + removed + 
                         ", New size: " + set.size() + 
                         ", Item no longer exists: " + !set.contains("item4"));
        
        // Test isEmpty
        System.out.println("Testing isEmpty operation...");
        boolean isEmpty = set.isEmpty();
        boolean isEmptyWorked = !isEmpty;
        if (isEmptyWorked) {
            System.out.println("✓ IsEmpty operation works correctly");
        } else {
            System.out.println("✗ IsEmpty operation failed");
        }
        recordTestResult("BasicSetOps-IsEmpty", isEmptyWorked, 
                         "IsEmpty operation test. Expected: false, Actual: " + isEmpty);
        
        // Test clear
        System.out.println("Testing clear operation...");
        set.clear();
        boolean clearWorked = set.isEmpty() && set.size() == 0;
        if (clearWorked) {
            System.out.println("✓ Clear operation works correctly");
        } else {
            System.out.println("✗ Clear operation failed");
        }
        recordTestResult("BasicSetOps-Clear", clearWorked, 
                         "Clear operation test. Is empty: " + set.isEmpty() + 
                         ", Size: " + set.size());
    }

    public void testSetUniqueElements() {
        System.out.println("\n=== Testing Set Uniqueness ===");
        
        // Test adding duplicate
        System.out.println("Testing uniqueness with duplicate add...");
        int initialSize = set.size();
        boolean added = set.add("item1"); // Already exists
        
        boolean uniquenessWorked = !added && set.size() == initialSize;
        if (uniquenessWorked) {
            System.out.println("✓ Set correctly rejected duplicate element");
        } else {
            System.out.println("✗ Set failed to maintain uniqueness");
        }
        recordTestResult("Uniqueness-DuplicateAdd", uniquenessWorked, 
                         "Duplicate add test. Add returned: " + added + 
                         ", Size unchanged: " + (set.size() == initialSize));
        
        // Test with null value
        System.out.println("Testing add null value...");
        boolean nullRejected = false;
        try {
            set.add(null);
            System.out.println("✗ Set should not allow null values");
        } catch (NullPointerException e) {
            nullRejected = true;
            System.out.println("✓ Set correctly rejected null value");
        }
        recordTestResult("Uniqueness-NullAdd", nullRejected, 
                         "Null add test. Null properly rejected: " + nullRejected);
    }

    public void testBulkOperations() {
        System.out.println("\n=== Testing Set Bulk Operations ===");
        
        // Test addAll
        System.out.println("Testing addAll operation...");
        Set<String> toAdd = new HashSet<>(Arrays.asList("item6", "item7", "item8"));
        boolean addedAll = set.addAll(toAdd);
        
        boolean addAllWorked = addedAll && set.size() == 8;
        if (addAllWorked) {
            System.out.println("✓ AddAll operation works correctly");
        } else {
            System.out.println("✗ AddAll operation failed");
        }
        recordTestResult("BulkOps-AddAll", addAllWorked, 
                         "AddAll operation test. AddAll returned: " + addedAll + 
                         ", Final size: " + set.size());
        
        // Test containsAll
        System.out.println("Testing containsAll operation...");
        Set<String> toCheck = new HashSet<>(Arrays.asList("item1", "item2"));
        boolean containsAll = set.containsAll(toCheck);
        
        boolean containsAllWorked = containsAll;
        if (containsAllWorked) {
            System.out.println("✓ ContainsAll operation works correctly");
        } else {
            System.out.println("✗ ContainsAll operation failed");
        }
        recordTestResult("BulkOps-ContainsAll", containsAllWorked, 
                         "ContainsAll operation test. Contains all required items: " + containsAll);
        
        // Test removeAll
        System.out.println("Testing removeAll operation...");
        Set<String> toRemove = new HashSet<>(Arrays.asList("item1", "item2", "nonexistent"));
        boolean removedAll = set.removeAll(toRemove);
        
        boolean removeAllWorked = removedAll && set.size() == 6 && 
                                 !set.contains("item1") && !set.contains("item2");
        if (removeAllWorked) {
            System.out.println("✓ RemoveAll operation works correctly");
        } else {
            System.out.println("✗ RemoveAll operation failed");
        }
        recordTestResult("BulkOps-RemoveAll", removeAllWorked, 
                         "RemoveAll operation test. RemoveAll returned: " + removedAll + 
                         ", Final size: " + set.size() + 
                         ", Items removed: " + (!set.contains("item1") && !set.contains("item2")));
        
        // Test retainAll
        System.out.println("Testing retainAll operation...");
        Set<String> toRetain = new HashSet<>(Arrays.asList("item3", "nonexistent"));
        boolean retained = set.retainAll(toRetain);
        
        boolean retainAllWorked = retained && set.size() == 1 && set.contains("item3");
        if (retainAllWorked) {
            System.out.println("✓ RetainAll operation works correctly");
        } else {
            System.out.println("✗ RetainAll operation failed");
        }
        recordTestResult("BulkOps-RetainAll", retainAllWorked, 
                         "RetainAll operation test. RetainAll returned: " + retained + 
                         ", Final size: " + set.size() + 
                         ", Contains item3: " + set.contains("item3"));
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing Set Listeners ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] itemAdded = new boolean[1];
        
        // Add an item listener
        System.out.println("Adding item listener...");
        UUID id = set.addItemListener(new ItemListener<String>() {
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
        boolean listenerWorked = received && itemAdded[0];
        
        if (listenerWorked) {
            System.out.println("✓ Set listener works correctly");
        } else {
            System.out.println("✗ Set listener failed or timed out");
        }
        recordTestResult("Listeners-EventNotification", listenerWorked, 
                         "Listener event notification test. Event received: " + received + 
                         ", Item added detected: " + itemAdded[0]);

        // Test listener removal
        System.out.println("Testing listener removal...");
        boolean removed = set.removeItemListener(id);
        
        if (removed) {
            System.out.println("✓ Listener removal worked correctly");
        } else {
            System.out.println("✗ Listener removal failed");
        }
        recordTestResult("Listeners-RemoveListener", removed, 
                         "Listener removal test. Successfully removed: " + removed);
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
        boolean concurrentModWorked = completed && set.size() == expectedSize;
        
        if (concurrentModWorked) {
            System.out.println("✓ Concurrent modifications successful. Final size: " + set.size());
        } else {
            System.out.println("✗ Concurrent modifications failed. Expected size: " + expectedSize + 
                              ", Actual size: " + set.size());
        }
        recordTestResult("ConcurrentMod-ThreadSafety", concurrentModWorked, 
                         "Concurrent modification test. Threads completed: " + completed + 
                         ", Expected size: " + expectedSize + 
                         ", Actual size: " + set.size());
    }
}
