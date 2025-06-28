package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapEvent;
import com.hazelcast.multimap.MultiMap;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test program for Hazelcast MultiMap functionality
 */
public class MultiMapTest extends AbstractTest {

    private MultiMap<String, String> multiMap;

    public MultiMapTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        multiMap = hazelcastInstance.getMultiMap("test-multimap");
    }

    @Override
    public void reset() {
        multiMap.clear();
        
        // Add test data
        multiMap.put("fruits", "apple");
        multiMap.put("fruits", "banana");
        multiMap.put("fruits", "orange");
        
        multiMap.put("vegetables", "carrot");
        multiMap.put("vegetables", "potato");
        
        multiMap.put("grains", "rice");
        multiMap.put("grains", "wheat");
        multiMap.put("grains", "barley");
    }

    @Override
    public void cleanup() {
        multiMap.destroy();
        multiMap = null;
    }

    public void testBasicMultiMapOperations() {
        System.out.println("\n=== Testing Basic MultiMap Operations ===");
        
        // Test size
        System.out.println("Testing size operation...");
        boolean sizeCorrect = multiMap.size() == 8;
        if (sizeCorrect) {
            System.out.println("✓ MultiMap size is correct: " + multiMap.size());
        } else {
            System.out.println("✗ MultiMap size is incorrect. Expected: 8, Actual: " + multiMap.size());
        }
        recordTestResult("BasicMultiMapOps-Size", sizeCorrect, 
                         "MultiMap size check. Expected: 8, Actual: " + multiMap.size());
        
        // Test valueCount
        System.out.println("Testing valueCount operation...");
        int fruitCount = multiMap.valueCount("fruits");
        boolean valueCountCorrect = fruitCount == 3;
        if (valueCountCorrect) {
            System.out.println("✓ ValueCount operation works correctly");
        } else {
            System.out.println("✗ ValueCount operation failed. Expected: 3, Actual: " + fruitCount);
        }
        recordTestResult("BasicMultiMapOps-ValueCount", valueCountCorrect, 
                         "ValueCount operation test. Expected: 3, Actual: " + fruitCount);
        
        // Test put
        System.out.println("Testing put operation...");
        boolean added = multiMap.put("fruits", "grape");
        boolean putWorked = added && multiMap.valueCount("fruits") == 4;
        if (putWorked) {
            System.out.println("✓ Put operation works correctly");
        } else {
            System.out.println("✗ Put operation failed");
        }
        recordTestResult("BasicMultiMapOps-Put", putWorked, 
                         "Put operation test. Added: " + added + ", New count: " + multiMap.valueCount("fruits"));
        
        // Test contains entry
        System.out.println("Testing containsEntry operation...");
        boolean containsEntry = multiMap.containsEntry("vegetables", "carrot");
        boolean doesNotContainEntry = multiMap.containsEntry("vegetables", "cucumber");
        boolean containsEntryWorked = containsEntry && !doesNotContainEntry;
        
        if (containsEntryWorked) {
            System.out.println("✓ ContainsEntry operation works correctly");
        } else {
            System.out.println("✗ ContainsEntry operation failed");
        }
        recordTestResult("BasicMultiMapOps-ContainsEntry", containsEntryWorked, 
                         "ContainsEntry operation test. Contains carrot: " + containsEntry + 
                         ", Contains cucumber: " + doesNotContainEntry);
        
        // Test contains key
        System.out.println("Testing containsKey operation...");
        boolean containsKey = multiMap.containsKey("grains");
        boolean doesNotContainKey = multiMap.containsKey("dairy");
        boolean containsKeyWorked = containsKey && !doesNotContainKey;
        
        if (containsKeyWorked) {
            System.out.println("✓ ContainsKey operation works correctly");
        } else {
            System.out.println("✗ ContainsKey operation failed");
        }
        recordTestResult("BasicMultiMapOps-ContainsKey", containsKeyWorked, 
                         "ContainsKey operation test. Contains grains: " + containsKey + 
                         ", Contains dairy: " + doesNotContainKey);
        
        // Test contains value
        System.out.println("Testing containsValue operation...");
        boolean containsValue = multiMap.containsValue("rice");
        boolean doesNotContainValue = multiMap.containsValue("milk");
        boolean containsValueWorked = containsValue && !doesNotContainValue;
        
        if (containsValueWorked) {
            System.out.println("✓ ContainsValue operation works correctly");
        } else {
            System.out.println("✗ ContainsValue operation failed");
        }
        recordTestResult("BasicMultiMapOps-ContainsValue", containsValueWorked, 
                         "ContainsValue operation test. Contains rice: " + containsValue + 
                         ", Contains milk: " + doesNotContainValue);
    }

    public void testValueCollection() {
        System.out.println("\n=== Testing MultiMap Value Collection ===");
        
        // Test get operation
        System.out.println("Testing get operation...");
        Collection<String> fruits = multiMap.get("fruits");
        boolean getWorked = fruits.size() == 3 && 
            fruits.contains("apple") && 
            fruits.contains("banana") && 
            fruits.contains("orange");
        
        if (getWorked) {
            System.out.println("✓ Get operation works correctly");
        } else {
            System.out.println("✗ Get operation failed");
        }
        recordTestResult("ValueCollection-Get", getWorked, 
                         "Get operation test. Collection size: " + fruits.size() + 
                         ", Contains all expected values: " + 
                         (fruits.contains("apple") && fruits.contains("banana") && 
                          fruits.contains("orange")));
        
        // Test values
        System.out.println("Testing values operation...");
        Collection<String> allValues = multiMap.values();
        boolean valuesWorked = allValues.size() == multiMap.size();
        
        if (valuesWorked) {
            System.out.println("✓ Values operation works correctly");
        } else {
            System.out.println("✗ Values operation failed");
        }
        recordTestResult("ValueCollection-Values", valuesWorked, 
                         "Values operation test. Expected size: " + multiMap.size() + 
                         ", Actual size: " + allValues.size());
    }

    public void testEntrySet() {
        System.out.println("\n=== Testing MultiMap Entry Set ===");
        
        // Test keySet
        System.out.println("Testing keySet operation...");
        Set<String> keys = multiMap.keySet();
        boolean keySetWorked = keys.size() == 3 && 
            keys.contains("fruits") && 
            keys.contains("vegetables") && 
            keys.contains("grains");
        
        if (keySetWorked) {
            System.out.println("✓ KeySet operation works correctly");
        } else {
            System.out.println("✗ KeySet operation failed");
        }
        recordTestResult("EntrySet-KeySet", keySetWorked, 
                         "KeySet operation test. Size: " + keys.size() + 
                         ", Contains all expected keys: " + 
                         (keys.contains("fruits") && keys.contains("vegetables") && keys.contains("grains")));
        
        // Test entrySet
        System.out.println("Testing entrySet operation...");
        Set<java.util.Map.Entry<String, String>> entries = multiMap.entrySet();
        boolean entrySetWorked = entries.size() == multiMap.size();
        
        if (entrySetWorked) {
            System.out.println("✓ EntrySet operation works correctly");
        } else {
            System.out.println("✗ EntrySet operation failed");
        }
        recordTestResult("EntrySet-EntrySet", entrySetWorked, 
                         "EntrySet operation test. Expected size: " + multiMap.size() + 
                         ", Actual size: " + entries.size());
    }

    public void testRemoveOperations() {
        System.out.println("\n=== Testing MultiMap Remove Operations ===");
        
        // Test remove specific entry
        System.out.println("Testing remove entry operation...");
        boolean removed = multiMap.remove("fruits", "banana");
        boolean removeEntryWorked = removed && multiMap.valueCount("fruits") == 2 && 
                                    !multiMap.containsEntry("fruits", "banana");
        
        if (removeEntryWorked) {
            System.out.println("✓ Remove entry operation works correctly");
        } else {
            System.out.println("✗ Remove entry operation failed");
        }
        recordTestResult("RemoveOps-RemoveEntry", removeEntryWorked, 
                         "Remove entry operation test. Removed: " + removed + 
                         ", New count: " + multiMap.valueCount("fruits") + 
                         ", Entry exists: " + multiMap.containsEntry("fruits", "banana"));
        
        // Test remove all entries for a key
        System.out.println("Testing remove all entries operation...");
        Collection<String> removedVegetables = multiMap.remove("vegetables");
        boolean removeAllWorked = removedVegetables.size() == 2 && 
                                 !multiMap.containsKey("vegetables") && 
                                 multiMap.keySet().size() == 2;
        
        if (removeAllWorked) {
            System.out.println("✓ Remove all entries operation works correctly");
        } else {
            System.out.println("✗ Remove all entries operation failed");
        }
        recordTestResult("RemoveOps-RemoveAll", removeAllWorked, 
                         "Remove all entries operation test. Removed count: " + removedVegetables.size() + 
                         ", Key exists: " + multiMap.containsKey("vegetables") + 
                         ", Remaining keys: " + multiMap.keySet().size());
        
        // Test clear
        System.out.println("Testing clear operation...");
        multiMap.clear();
        boolean clearWorked = multiMap.size() == 0 && multiMap.keySet().isEmpty();
        
        if (clearWorked) {
            System.out.println("✓ Clear operation works correctly");
        } else {
            System.out.println("✗ Clear operation failed");
        }
        recordTestResult("RemoveOps-Clear", clearWorked, 
                         "Clear operation test. Size after clear: " + multiMap.size() + 
                         ", KeySet empty: " + multiMap.keySet().isEmpty());
        
        // Reset data for next tests
        setup();
    }

    public void testLockOperations() throws Exception {
        System.out.println("\n=== Testing MultiMap Lock Operations ===");
        
        // Test lock
        System.out.println("Testing lock operation...");
        multiMap.lock("fruits");
        
        boolean lockWorked = false;
        try {
            // Create a thread that tries to modify the locked key
            final boolean[] threadModified = new boolean[1];
            final CountDownLatch latch = new CountDownLatch(1);
            
            Thread lockTestThread = new Thread(() -> {
                try {
                    // Try to modify the locked key
                    multiMap.put("fruits", "kiwi");
                    threadModified[0] = true;
                } finally {
                    latch.countDown();
                }
            });
            
            // Start thread but don't wait too long to avoid test hang
            lockTestThread.start();
            boolean threadCompleted = latch.await(1, TimeUnit.SECONDS);
            
            // If thread completed quickly, lock might not have worked
            lockWorked = !(threadCompleted && threadModified[0]);
            if (lockWorked) {
                System.out.println("✓ Lock operation works correctly");
            } else {
                System.out.println("✗ Lock operation failed - another thread was able to modify");
            }
        } finally {
            // Unlock to allow test to continue
            multiMap.unlock("fruits");
        }
        recordTestResult("LockOps-Lock", lockWorked, 
                         "Lock operation test. Lock prevented modification: " + lockWorked);
        
        // Test tryLock
        System.out.println("Testing tryLock operation...");
        boolean locked = multiMap.tryLock("grains");
        boolean unlockWorked = false;
        
        if (locked) {
            try {
                System.out.println("✓ TryLock operation works correctly");
                recordTestResult("LockOps-TryLock", true, 
                                "TryLock operation test. Successfully acquired lock");
                
                // Test unlock
                multiMap.unlock("grains");
                
                // Try to lock again to confirm unlock worked
                boolean lockedAgain = multiMap.tryLock("grains");
                unlockWorked = lockedAgain;
                if (unlockWorked) {
                    System.out.println("✓ Unlock operation works correctly");
                    multiMap.unlock("grains");
                } else {
                    System.out.println("✗ Unlock operation failed");
                }
            } finally {
                // Ensure we always unlock
                if (multiMap.isLocked("grains")) {
                    multiMap.unlock("grains");
                }
            }
        } else {
            System.out.println("✗ TryLock operation failed");
            recordTestResult("LockOps-TryLock", false, 
                            "TryLock operation test. Failed to acquire lock");
        }
        recordTestResult("LockOps-Unlock", unlockWorked, 
                         "Unlock operation test. Successfully unlocked and reacquired: " + unlockWorked);
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing MultiMap Listeners ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] entryAdded = new boolean[1];
        
        // Add an entry listener
        System.out.println("Adding entry listener...");
        UUID id = multiMap.addEntryListener(new EntryListener<String, String>() {
            @Override
            public void entryAdded(EntryEvent<String, String> event) {
                System.out.println("Entry added: " + event.getKey() + " -> " + event.getValue());
                entryAdded[0] = true;
                latch.countDown();
            }

            @Override
            public void entryRemoved(EntryEvent<String, String> event) {}

            @Override
            public void entryUpdated(EntryEvent<String, String> event) {}

            @Override
            public void entryEvicted(EntryEvent<String, String> event) {}

            @Override
            public void entryExpired(EntryEvent<String, String> event) {}

            @Override
            public void mapCleared(MapEvent event) {}

            @Override
            public void mapEvicted(MapEvent event) {}
        }, true);
        
        // Add an entry to trigger the listener
        System.out.println("Adding entry to trigger listener...");
        multiMap.put("newCategory", "newItem");
        
        // Wait for the listener to be triggered
        boolean received = latch.await(5, TimeUnit.SECONDS);
        boolean listenerWorked = received && entryAdded[0];
        
        if (listenerWorked) {
            System.out.println("✓ MultiMap listener works correctly");
        } else {
            System.out.println("✗ MultiMap listener failed or timed out");
        }
        recordTestResult("Listeners-EventNotification", listenerWorked, 
                        "Listener event notification test. Event received: " + received + 
                        ", Entry added detected: " + entryAdded[0]);

        // Test listener removal
        System.out.println("Testing listener removal...");
        boolean removed = multiMap.removeEntryListener(id);
        
        if (removed) {
            System.out.println("✓ Listener removal worked correctly");
        } else {
            System.out.println("✗ Listener removal failed");
        }
        recordTestResult("Listeners-RemoveListener", removed, 
                        "Listener removal test. Successfully removed: " + removed);
    }
}
