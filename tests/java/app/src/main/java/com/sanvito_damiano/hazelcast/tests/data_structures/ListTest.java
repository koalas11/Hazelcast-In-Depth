package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test program for Hazelcast IList operations
 */
public class ListTest extends AbstractTest {

    private IList<String> list;

    public ListTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        list = hazelcastInstance.getList("test-list");
    }

    @Override
    public void reset() {
        list.clear();
        // Add some initial data
        list.add("item1");
        list.add("item2");
        list.add("item3");
        list.add("item4");
        list.add("item5");
    }

    @Override
    public void cleanup() {
        list = hazelcastInstance.getList("test-list");
        list.destroy();
        list = null;
    }

    public void testBasicListOperations() {
        System.out.println("\n=== Testing Basic List Operations ===");
        
        // Test size
        System.out.println("Testing size operation...");
        boolean sizeCorrect = list.size() == 5;
        if (sizeCorrect) {
            System.out.println("✓ List size is correct: " + list.size());
        } else {
            System.out.println("✗ List size is incorrect. Expected: 5, Actual: " + list.size());
        }
        recordTestResult("BasicListOps-Size", sizeCorrect, 
                         "List size check. Expected: 5, Actual: " + list.size());
        
        // Test get
        System.out.println("Testing get operation...");
        String item = list.get(2);
        boolean getOpWorks = "item3".equals(item);
        if (getOpWorks) {
            System.out.println("✓ Get operation works correctly");
        } else {
            System.out.println("✗ Get operation failed. Expected: item3, Actual: " + item);
        }
        recordTestResult("BasicListOps-Get", getOpWorks, 
                         "Get operation test. Expected: item3, Actual: " + item);
        
        // Test contains
        System.out.println("Testing contains operation...");
        boolean containsItem = list.contains("item2");
        boolean doesNotContain = list.contains("nonexistent");
        boolean containsOpWorks = containsItem && !doesNotContain;
        if (containsOpWorks) {
            System.out.println("✓ Contains operation works correctly");
        } else {
            System.out.println("✗ Contains operation failed");
        }
        recordTestResult("BasicListOps-Contains", containsOpWorks, 
                         "Contains operation test. Contains 'item2': " + containsItem + 
                         ", Contains 'nonexistent': " + doesNotContain);
        
        // Test add
        System.out.println("Testing add operation...");
        list.add("item6");
        boolean addOpWorks = list.size() == 6 && list.get(5).equals("item6");
        if (addOpWorks) {
            System.out.println("✓ Add operation works correctly");
        } else {
            System.out.println("✗ Add operation failed");
        }
        recordTestResult("BasicListOps-Add", addOpWorks, 
                         "Add operation test. List size: " + list.size() + 
                         ", Last item: " + list.get(5));
        
        // Test remove by object
        System.out.println("Testing remove by object operation...");
        boolean removed = list.remove("item4");
        boolean removeByObjectWorks = removed && list.size() == 5 && !list.contains("item4");
        if (removeByObjectWorks) {
            System.out.println("✓ Remove by object operation works correctly");
        } else {
            System.out.println("✗ Remove by object operation failed");
        }
        recordTestResult("BasicListOps-RemoveByObject", removeByObjectWorks, 
                         "Remove by object operation test. Removed: " + removed + 
                         ", List size: " + list.size() + 
                         ", Contains 'item4': " + list.contains("item4"));
        
        // Test remove by index
        System.out.println("Testing remove by index operation...");
        String removedItem = list.remove(0);
        boolean removeByIndexWorks = "item1".equals(removedItem) && list.size() == 4 && list.get(0).equals("item2");
        if (removeByIndexWorks) {
            System.out.println("✓ Remove by index operation works correctly");
        } else {
            System.out.println("✗ Remove by index operation failed");
        }
        recordTestResult("BasicListOps-RemoveByIndex", removeByIndexWorks, 
                         "Remove by index operation test. Removed item: " + removedItem + 
                         ", List size: " + list.size() + 
                         ", First item: " + list.get(0));
    }

    public void testListIteration() {
        System.out.println("\n=== Testing List Iteration ===");
        
        System.out.println("Testing foreach iteration...");
        StringBuilder result = new StringBuilder();
        for (String item : list) {
            result.append(item).append(",");
        }
        
        boolean iterationWorks = result.toString().equals("item1,item2,item3,item4,item5,");
        if (iterationWorks) {
            System.out.println("✓ List iteration works correctly");
        } else {
            System.out.println("✗ List iteration failed. Result: " + result.toString());
        }
        recordTestResult("ListIteration-Foreach", iterationWorks, 
                         "List iteration test. Expected: item1,item2,item3,item4,item5, Actual: " + result.toString());
    }

    public void testIndexOperations() {
        System.out.println("\n=== Testing List Index Operations ===");
        
        // Test add at index
        System.out.println("Testing add at index operation...");
        list.add(2, "inserted");
        boolean addAtIndexWorks = list.size() == 6 && list.get(2).equals("inserted");
        if (addAtIndexWorks) {
            System.out.println("✓ Add at index operation works correctly");
        } else {
            System.out.println("✗ Add at index operation failed");
        }
        recordTestResult("IndexOps-AddAtIndex", addAtIndexWorks, 
                         "Add at index operation test. List size: " + list.size() + 
                         ", Item at index 2: " + list.get(2));
        
        // Test set
        System.out.println("Testing set operation...");
        String oldValue = list.set(1, "replaced");
        boolean setOpWorks = "item2".equals(oldValue) && list.get(1).equals("replaced");
        if (setOpWorks) {
            System.out.println("✓ Set operation works correctly");
        } else {
            System.out.println("✗ Set operation failed");
        }
        recordTestResult("IndexOps-Set", setOpWorks, 
                         "Set operation test. Old value: " + oldValue + 
                         ", New value at index 1: " + list.get(1));
        
        // Test indexOf
        System.out.println("Testing indexOf operation...");
        int index = list.indexOf("item4");
        boolean indexOfWorks = index == 4;
        if (indexOfWorks) {
            System.out.println("✓ IndexOf operation works correctly");
        } else {
            System.out.println("✗ IndexOf operation failed. Expected: 4, Actual: " + index);
        }
        recordTestResult("IndexOps-IndexOf", indexOfWorks, 
                         "IndexOf operation test. Expected: 4, Actual: " + index);
        
        // Test lastIndexOf
        System.out.println("Testing lastIndexOf operation...");
        list.add("item5"); // Add duplicate
        int lastIndex = list.lastIndexOf("item5");
        boolean lastIndexOfWorks = lastIndex == 6;
        if (lastIndexOfWorks) {
            System.out.println("✓ LastIndexOf operation works correctly");
        } else {
            System.out.println("✗ LastIndexOf operation failed. Expected: 6, Actual: " + lastIndex);
        }
        recordTestResult("IndexOps-LastIndexOf", lastIndexOfWorks, 
                         "LastIndexOf operation test. Expected: 6, Actual: " + lastIndex);
    }

    public void testBulkOperations() {
        System.out.println("\n=== Testing List Bulk Operations ===");
        
        // Test addAll
        System.out.println("Testing addAll operation...");
        List<String> toAdd = new ArrayList<>();
        toAdd.add("bulk1");
        toAdd.add("bulk2");
        list.addAll(toAdd);
        boolean addAllWorks = list.size() == 7 && list.get(5).equals("bulk1") && list.get(6).equals("bulk2");
        if (addAllWorks) {
            System.out.println("✓ AddAll operation works correctly");
        } else {
            System.out.println("✗ AddAll operation failed");
        }
        recordTestResult("BulkOps-AddAll", addAllWorks, 
                         "AddAll operation test. List size: " + list.size() + 
                         ", Item at index 5: " + list.get(5) + 
                         ", Item at index 6: " + list.get(6));
        
        // Test addAll at index
        System.out.println("Testing addAll at index operation...");
        List<String> toAddIndex = new ArrayList<>();
        toAddIndex.add("index1");
        toAddIndex.add("index2");
        list.addAll(2, toAddIndex);
        boolean addAllAtIndexWorks = list.size() == 9 && list.get(2).equals("index1") && list.get(3).equals("index2");
        if (addAllAtIndexWorks) {
            System.out.println("✓ AddAll at index operation works correctly");
        } else {
            System.out.println("✗ AddAll at index operation failed");
        }
        recordTestResult("BulkOps-AddAllAtIndex", addAllAtIndexWorks, 
                         "AddAll at index operation test. List size: " + list.size() + 
                         ", Item at index 2: " + list.get(2) + 
                         ", Item at index 3: " + list.get(3));
        
        // Test removeAll
        System.out.println("Testing removeAll operation...");
        List<String> toRemove = new ArrayList<>();
        toRemove.add("bulk1");
        toRemove.add("bulk2");
        list.removeAll(toRemove);
        boolean removeAllWorks = list.size() == 7 && !list.contains("bulk1") && !list.contains("bulk2");
        if (removeAllWorks) {
            System.out.println("✓ RemoveAll operation works correctly");
        } else {
            System.out.println("✗ RemoveAll operation failed");
        }
        recordTestResult("BulkOps-RemoveAll", removeAllWorks, 
                         "RemoveAll operation test. List size: " + list.size() + 
                         ", Contains 'bulk1': " + list.contains("bulk1") + 
                         ", Contains 'bulk2': " + list.contains("bulk2"));
        
        // Test retainAll
        System.out.println("Testing retainAll operation...");
        List<String> toRetain = new ArrayList<>();
        toRetain.add("item1");
        toRetain.add("item2");
        list.retainAll(toRetain);
        boolean retainAllWorks = list.size() == 2 && list.contains("item1") && list.contains("item2");
        if (retainAllWorks) {
            System.out.println("✓ RetainAll operation works correctly");
        } else {
            System.out.println("✗ RetainAll operation failed");
        }
        recordTestResult("BulkOps-RetainAll", retainAllWorks, 
                         "RetainAll operation test. List size: " + list.size() + 
                         ", Contains 'item1': " + list.contains("item1") + 
                         ", Contains 'item2': " + list.contains("item2"));
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing List Listeners ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] itemAdded = new boolean[1];
        
        // Add an item listener
        System.out.println("Adding item listener...");
        list.addItemListener(new ItemListener<String>() {
            @Override
            public void itemAdded(ItemEvent<String> event) {
                if (event.getItem().equals("new-item")) {
                    itemAdded[0] = true;
                    latch.countDown();
                }
            }

            @Override
            public void itemRemoved(ItemEvent<String> event) {
                // Not needed for this test
            }
        }, true);
        
        // Add an item to trigger the listener
        System.out.println("Adding item to trigger listener...");
        list.add("new-item");
        
        // Wait for the listener to be triggered
        boolean received = latch.await(5, TimeUnit.SECONDS);
        boolean listenerWorks = received && itemAdded[0];
        
        if (listenerWorks) {
            System.out.println("✓ List listener works correctly");
        } else {
            System.out.println("✗ List listener failed or timed out");
        }
        recordTestResult("Listeners-ItemAdded", listenerWorks, 
                         "List listener test. Event received: " + received + 
                         ", Correct item: " + itemAdded[0]);
    }

    public void testConcurrentModification() throws Exception {
        System.out.println("\n=== Testing Concurrent List Modifications ===");

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
                    
                    // Each thread adds 20 items
                    for (int j = 0; j < 20; j++) {
                        list.add("thread" + threadId + "-item" + j);
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
        boolean concurrentModsSuccessful = completed && list.size() == expectedSize;
        
        if (concurrentModsSuccessful) {
            System.out.println("✓ Concurrent modifications successful. Final size: " + list.size());
        } else {
            System.out.println("✗ Concurrent modifications failed. Expected size: " + expectedSize + 
                              ", Actual size: " + list.size());
        }
        recordTestResult("Concurrency-MultiThreaded", concurrentModsSuccessful, 
                         "Concurrent modifications test. All threads completed: " + completed + 
                         ", Expected size: " + expectedSize + 
                         ", Actual size: " + list.size());
    }
}
