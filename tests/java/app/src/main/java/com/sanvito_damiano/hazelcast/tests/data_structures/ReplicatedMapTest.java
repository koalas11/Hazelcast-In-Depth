package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapEvent;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test program for Hazelcast ReplicatedMap functionality
 */
public class ReplicatedMapTest extends AbstractTest {

    private ReplicatedMap<String, String> replicatedMap;

    public ReplicatedMapTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        replicatedMap = hazelcastInstance.getReplicatedMap("test-replicated-map");
    }

    @Override
    public void reset() {
        replicatedMap.clear();
        
        // Add test data
        replicatedMap.put("key1", "value1");
        replicatedMap.put("key2", "value2");
        replicatedMap.put("key3", "value3");
        replicatedMap.put("key4", "value4");
        replicatedMap.put("key5", "value5");
    }

    @Override
    public void cleanup() {
        replicatedMap.destroy();
        replicatedMap = null;
    }

    public void testBasicMapOperations() {
        System.out.println("\n=== Testing Basic ReplicatedMap Operations ===");
        
        // Test size
        System.out.println("Testing size operation...");
        boolean sizeCorrect = replicatedMap.size() == 5;
        if (sizeCorrect) {
            System.out.println("✓ ReplicatedMap size is correct: " + replicatedMap.size());
        } else {
            System.out.println("✗ ReplicatedMap size is incorrect. Expected: 5, Actual: " + replicatedMap.size());
        }
        recordTestResult("BasicMapOps-Size", sizeCorrect, 
                         "ReplicatedMap size check. Expected: 5, Actual: " + replicatedMap.size());
        
        // Test get
        System.out.println("Testing get operation...");
        String value = replicatedMap.get("key3");
        boolean getWorked = "value3".equals(value);
        if (getWorked) {
            System.out.println("✓ Get operation works correctly");
        } else {
            System.out.println("✗ Get operation failed. Expected: value3, Actual: " + value);
        }
        recordTestResult("BasicMapOps-Get", getWorked, 
                         "Get operation test. Expected: value3, Actual: " + value);
        
        // Test containsKey
        System.out.println("Testing containsKey operation...");
        boolean containsKey = replicatedMap.containsKey("key2");
        boolean doesNotContainKey = replicatedMap.containsKey("nonexistent");
        boolean containsKeyWorked = containsKey && !doesNotContainKey;
        
        if (containsKeyWorked) {
            System.out.println("✓ ContainsKey operation works correctly");
        } else {
            System.out.println("✗ ContainsKey operation failed");
        }
        recordTestResult("BasicMapOps-ContainsKey", containsKeyWorked, 
                         "ContainsKey operation test. Contains key2: " + containsKey + 
                         ", Contains nonexistent: " + doesNotContainKey);
        
        // Test containsValue
        System.out.println("Testing containsValue operation...");
        boolean containsValue = replicatedMap.containsValue("value4");
        boolean doesNotContainValue = replicatedMap.containsValue("nonexistent");
        boolean containsValueWorked = containsValue && !doesNotContainValue;
        
        if (containsValueWorked) {
            System.out.println("✓ ContainsValue operation works correctly");
        } else {
            System.out.println("✗ ContainsValue operation failed");
        }
        recordTestResult("BasicMapOps-ContainsValue", containsValueWorked, 
                         "ContainsValue operation test. Contains value4: " + containsValue + 
                         ", Contains nonexistent: " + doesNotContainValue);
        
        // Test put
        System.out.println("Testing put operation...");
        String oldValue = replicatedMap.put("key6", "value6");
        boolean putWorked = oldValue == null && replicatedMap.size() == 6 && 
                           "value6".equals(replicatedMap.get("key6"));
        
        if (putWorked) {
            System.out.println("✓ Put operation works correctly");
        } else {
            System.out.println("✗ Put operation failed");
        }
        recordTestResult("BasicMapOps-Put", putWorked, 
                         "Put operation test. Old value null: " + (oldValue == null) + 
                         ", New size: " + replicatedMap.size() + 
                         ", New value correct: " + "value6".equals(replicatedMap.get("key6")));
        
        // Test remove
        System.out.println("Testing remove operation...");
        String removed = replicatedMap.remove("key5");
        boolean removeWorked = "value5".equals(removed) && replicatedMap.size() == 5 && 
                              !replicatedMap.containsKey("key5");
        
        if (removeWorked) {
            System.out.println("✓ Remove operation works correctly");
        } else {
            System.out.println("✗ Remove operation failed");
        }
        recordTestResult("BasicMapOps-Remove", removeWorked, 
                         "Remove operation test. Removed value: " + removed + 
                         ", New size: " + replicatedMap.size() + 
                         ", Key no longer exists: " + !replicatedMap.containsKey("key5"));
    }

    public void testEntrySetOperations() {
        System.out.println("\n=== Testing ReplicatedMap Entry Set Operations ===");
        
        // Test keySet
        System.out.println("Testing keySet operation...");
        Set<String> keys = replicatedMap.keySet();
        boolean keySetWorked = keys.size() == 5 && 
                              keys.contains("key1") && 
                              keys.contains("key2") && 
                              keys.contains("key3") && 
                              keys.contains("key4") && 
                              keys.contains("key6");
        
        if (keySetWorked) {
            System.out.println("✓ KeySet operation works correctly");
        } else {
            System.out.println("✗ KeySet operation failed");
        }
        recordTestResult("EntrySetOps-KeySet", keySetWorked, 
                         "KeySet operation test. Size: " + keys.size() + 
                         ", Contains expected keys: " + (keys.contains("key1") && 
                                                        keys.contains("key2") && 
                                                        keys.contains("key3") && 
                                                        keys.contains("key4") && 
                                                        keys.contains("key6")));
        
        // Test values
        System.out.println("Testing values operation...");
        boolean valuesCorrect = replicatedMap.values().size() == 5 && 
                               replicatedMap.values().contains("value1") &&
                               replicatedMap.values().contains("value6");
        
        if (valuesCorrect) {
            System.out.println("✓ Values operation works correctly");
        } else {
            System.out.println("✗ Values operation failed");
        }
        recordTestResult("EntrySetOps-Values", valuesCorrect, 
                         "Values operation test. Size: " + replicatedMap.values().size() + 
                         ", Contains expected values: " + (replicatedMap.values().contains("value1") && 
                                                          replicatedMap.values().contains("value6")));
        
        // Test entrySet
        System.out.println("Testing entrySet operation...");
        Set<Map.Entry<String, String>> entries = replicatedMap.entrySet();
        boolean entrySetSizeCorrect = entries.size() == 5;
        boolean entryValuesCorrect = true;
        
        if (entrySetSizeCorrect) {
            for (Map.Entry<String, String> entry : entries) {
                String expectedValue = replicatedMap.get(entry.getKey());
                if (!entry.getValue().equals(expectedValue)) {
                    entryValuesCorrect = false;
                    break;
                }
            }
            
            if (entryValuesCorrect) {
                System.out.println("✓ EntrySet operation works correctly");
            } else {
                System.out.println("✗ EntrySet operation failed - values don't match");
            }
        } else {
            System.out.println("✗ EntrySet operation failed - wrong size");
            entryValuesCorrect = false;
        }
        
        boolean entrySetWorked = entrySetSizeCorrect && entryValuesCorrect;
        recordTestResult("EntrySetOps-EntrySet", entrySetWorked, 
                         "EntrySet operation test. Size correct: " + entrySetSizeCorrect + 
                         ", Values match: " + entryValuesCorrect);
    }

    public void testBulkOperations() {
        System.out.println("\n=== Testing ReplicatedMap Bulk Operations ===");
        
        // Test putAll
        System.out.println("Testing putAll operation...");
        Map<String, String> mapToAdd = new HashMap<>();
        mapToAdd.put("bulk1", "bulkValue1");
        mapToAdd.put("bulk2", "bulkValue2");
        mapToAdd.put("bulk3", "bulkValue3");
        
        replicatedMap.putAll(mapToAdd);
        
        boolean putAllWorked = replicatedMap.size() == 8 && 
                              "bulkValue1".equals(replicatedMap.get("bulk1")) && 
                              "bulkValue2".equals(replicatedMap.get("bulk2")) && 
                              "bulkValue3".equals(replicatedMap.get("bulk3"));
        
        if (putAllWorked) {
            System.out.println("✓ PutAll operation works correctly");
        } else {
            System.out.println("✗ PutAll operation failed");
        }
        recordTestResult("BulkOps-PutAll", putAllWorked, 
                         "PutAll operation test. New size: " + replicatedMap.size() + 
                         ", Contains new entries: " + ("bulkValue1".equals(replicatedMap.get("bulk1")) && 
                                                      "bulkValue2".equals(replicatedMap.get("bulk2")) && 
                                                      "bulkValue3".equals(replicatedMap.get("bulk3"))));
        
        // Test clear
        System.out.println("Testing clear operation...");
        replicatedMap.clear();
        boolean clearWorked = replicatedMap.isEmpty() && replicatedMap.size() == 0;
        
        if (clearWorked) {
            System.out.println("✓ Clear operation works correctly");
        } else {
            System.out.println("✗ Clear operation failed");
        }
        recordTestResult("BulkOps-Clear", clearWorked, 
                         "Clear operation test. Map empty: " + replicatedMap.isEmpty() + 
                         ", Size zero: " + (replicatedMap.size() == 0));
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing ReplicatedMap Listeners ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] entryAdded = new boolean[1];
        
        // Add an entry listener
        System.out.println("Adding entry listener...");
        UUID id = replicatedMap.addEntryListener(new EntryListener<String, String>() {
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
        }, "newKey");
        
        // Add an entry to trigger the listener
        System.out.println("Adding entry to trigger listener...");
        replicatedMap.put("newKey", "newValue");
        
        // Wait for the listener to be triggered
        boolean received = latch.await(5, TimeUnit.SECONDS);
        boolean listenerWorked = received && entryAdded[0];
        
        if (listenerWorked) {
            System.out.println("✓ ReplicatedMap listener works correctly");
        } else {
            System.out.println("✗ ReplicatedMap listener failed or timed out");
        }
        recordTestResult("Listeners-EventNotification", listenerWorked, 
                         "Listener event notification test. Event received: " + received + 
                         ", Entry added detected: " + entryAdded[0]);

        // Test listener removal
        System.out.println("Testing listener removal...");
        boolean removed = replicatedMap.removeEntryListener(id);
        
        if (removed) {
            System.out.println("✓ Listener removal worked correctly");
        } else {
            System.out.println("✗ Listener removal failed");
        }
        recordTestResult("Listeners-RemoveListener", removed, 
                         "Listener removal test. Successfully removed: " + removed);
    }

    public void testPutWithTTL() throws Exception {
        System.out.println("\n=== Testing ReplicatedMap Put with TTL ===");
        
        // Put with TTL
        System.out.println("Adding entry with 2 seconds TTL...");
        replicatedMap.put("expiring", "expiringValue", 2, TimeUnit.SECONDS);
        
        // Verify entry exists
        boolean entryAdded = replicatedMap.containsKey("expiring");
        if (entryAdded) {
            System.out.println("✓ Entry added with TTL");
        } else {
            System.out.println("✗ Failed to add entry with TTL");
            recordTestResult("TTL-AddEntry", false, 
                             "TTL entry add test. Entry exists: " + entryAdded);
            return;
        }
        recordTestResult("TTL-AddEntry", true, 
                         "TTL entry add test. Entry exists: " + entryAdded);
        
        // Wait half the TTL time
        System.out.println("Waiting for 1 second...");
        Thread.sleep(1000);
        
        // Entry should still exist
        boolean existsHalfway = replicatedMap.containsKey("expiring");
        if (existsHalfway) {
            System.out.println("✓ Entry still exists after half TTL period");
        } else {
            System.out.println("✗ Entry expired too early");
            recordTestResult("TTL-HalfwayCheck", false, 
                             "TTL halfway check test. Entry still exists: " + existsHalfway);
            return;
        }
        recordTestResult("TTL-HalfwayCheck", true, 
                         "TTL halfway check test. Entry still exists: " + existsHalfway);
        
        // Wait until after TTL
        System.out.println("Waiting for 1.5 more seconds...");
        Thread.sleep(1500);
        
        // Entry should be gone
        boolean expiredCorrectly = !replicatedMap.containsKey("expiring");
        if (expiredCorrectly) {
            System.out.println("✓ Entry expired correctly after TTL");
        } else {
            System.out.println("✗ Entry did not expire as expected");
        }
        recordTestResult("TTL-ExpiredCheck", expiredCorrectly, 
                         "TTL expired check test. Entry correctly expired: " + expiredCorrectly);
    }
}
