package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test program for Hazelcast Map functionality
 */
public class MapTest extends AbstractTest {

    private IMap<String, Person> personMap;

    public MapTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
        this.testCategory = testCategory;
    }

    @Override
    public void setup() {
        personMap = hazelcastInstance.getMap("persons");
    }

    @Override
    public void reset() {
        personMap.clear();
        // Add test data
        personMap.put("p1", new Person("Alice", 32, true));
        personMap.put("p2", new Person("Bob", 24, true));
        personMap.put("p3", new Person("Charlie", 29, true));
        personMap.put("p4", new Person("Diana", 41, false));
        personMap.put("p5", new Person("Edward", 18, false));
    }

    @Override
    public void cleanup() {
        personMap = hazelcastInstance.getMap("persons");
        personMap.destroy();
        personMap = null;
    }

    public void testBasicMapOperations() {
        System.out.println("\n=== Testing Basic Map Operations ===");
        
        // Test map size
        System.out.println("Testing map size...");
        boolean mapSizeCorrect = personMap.size() == 5;
        if (mapSizeCorrect) {
            System.out.println("✓ Map size is correct: " + personMap.size());
        } else {
            System.out.println("✗ Map size is incorrect. Expected: 5, Actual: " + personMap.size());
        }
        recordTestResult("BasicMapOps-Size", mapSizeCorrect, 
                         "Map size check. Expected: 5, Actual: " + personMap.size());
        
        // Test get operation
        System.out.println("Testing get operation...");
        Person alice = personMap.get("p1");
        boolean getOpWorks = "Alice".equals(alice.getName()) && alice.getAge() == 32;
        if (getOpWorks) {
            System.out.println("✓ Get operation works correctly");
        } else {
            System.out.println("✗ Get operation failed. Expected: Alice(32), Actual: " + alice);
        }
        recordTestResult("BasicMapOps-Get", getOpWorks, 
                         "Get operation test. Expected: Alice(32), Actual: " + alice);
        
        // Test put/replace
        System.out.println("Testing put operation...");
        personMap.put("p6", new Person("Frank", 50, true));
        boolean putOpWorks = personMap.size() == 6 && personMap.containsKey("p6");
        if (putOpWorks) {
            System.out.println("✓ Put operation works correctly");
        } else {
            System.out.println("✗ Put operation failed. Expected size: 6, Actual: " + personMap.size());
        }
        recordTestResult("BasicMapOps-Put", putOpWorks, 
                         "Put operation test. Expected size: 6, Actual: " + personMap.size());
        
        // Test replace operation
        System.out.println("Testing replace operation...");
        Person originalBob = personMap.get("p2");
        Person newBob = new Person("Bob", 25, true); // Bob had a birthday
        personMap.replace("p2", newBob);
        Person updatedBob = personMap.get("p2");
        
        boolean replaceOpWorks = updatedBob.getAge() == 25 && originalBob.getAge() == 24;
        if (replaceOpWorks) {
            System.out.println("✓ Replace operation works correctly");
        } else {
            System.out.println("✗ Replace operation failed");
        }
        recordTestResult("BasicMapOps-Replace", replaceOpWorks, 
                         "Replace operation test. Original age: " + originalBob.getAge() + ", New age: " + updatedBob.getAge());
        
        // Test conditional replace
        System.out.println("Testing conditional replace operation...");
        Person originalCharlie = personMap.get("p3");
        Person newCharlie = new Person("Charlie", 30, true);
        boolean replaceSuccess = personMap.replace("p3", originalCharlie, newCharlie);
        
        boolean condReplaceOpWorks = replaceSuccess && personMap.get("p3").getAge() == 30;
        if (condReplaceOpWorks) {
            System.out.println("✓ Conditional replace operation works correctly");
        } else {
            System.out.println("✗ Conditional replace operation failed");
        }
        recordTestResult("BasicMapOps-ConditionalReplace", condReplaceOpWorks, 
                         "Conditional replace operation test. Success: " + replaceSuccess + ", New age: " + personMap.get("p3").getAge());
        
        // Test containsKey
        System.out.println("Testing containsKey operation...");
        boolean containsP3 = personMap.containsKey("p3");
        boolean containsNonexistent = personMap.containsKey("nonexistent");
        boolean containsKeyOpWorks = containsP3 && !containsNonexistent;
        if (containsKeyOpWorks) {
            System.out.println("✓ ContainsKey operation works correctly");
        } else {
            System.out.println("✗ ContainsKey operation failed");
        }
        recordTestResult("BasicMapOps-ContainsKey", containsKeyOpWorks, 
                         "ContainsKey operation test. Contains p3: " + containsP3 + ", Contains nonexistent: " + containsNonexistent);
        
        // Test containsValue
        System.out.println("Testing containsValue operation...");
        boolean containsAlice = personMap.containsValue(new Person("Alice", 32, true));
        boolean containsNonexistentPerson = personMap.containsValue(new Person("NonExistent", 99, false));
        boolean containsValueOpWorks = containsAlice && !containsNonexistentPerson;
        
        if (containsValueOpWorks) {
            System.out.println("✓ ContainsValue operation works correctly");
        } else {
            System.out.println("✗ ContainsValue operation failed");
        }
        recordTestResult("BasicMapOps-ContainsValue", containsValueOpWorks, 
                         "ContainsValue operation test. Contains Alice: " + containsAlice + ", Contains nonexistent: " + containsNonexistentPerson);
        
        // Test remove
        System.out.println("Testing remove operation...");
        Person removed = personMap.remove("p5");
        boolean removeOpWorks = removed != null && "Edward".equals(removed.getName()) && !personMap.containsKey("p5");
        
        if (removeOpWorks) {
            System.out.println("✓ Remove operation works correctly");
        } else {
            System.out.println("✗ Remove operation failed");
        }
        recordTestResult("BasicMapOps-Remove", removeOpWorks, 
                         "Remove operation test. Removed person: " + (removed != null ? removed.getName() : "null"));
        
        // Test conditional remove
        System.out.println("Testing conditional remove operation...");
        Person originalFrank = personMap.get("p6");
        boolean removeSuccess = personMap.remove("p6", originalFrank);
        boolean condRemoveOpWorks = removeSuccess && !personMap.containsKey("p6");
        
        if (condRemoveOpWorks) {
            System.out.println("✓ Conditional remove operation works correctly");
        } else {
            System.out.println("✗ Conditional remove operation failed");
        }
        recordTestResult("BasicMapOps-ConditionalRemove", condRemoveOpWorks, 
                         "Conditional remove operation test. Success: " + removeSuccess);
        
        // Test keySet, values, entrySet
        System.out.println("Testing collection views...");
        Set<String> keys = personMap.keySet();
        Collection<Person> values = personMap.values();
        Set<Map.Entry<String, Person>> entries = personMap.entrySet();
        boolean collectionViewsWork = keys.size() == 4 && values.size() == 4 && entries.size() == 4;
        
        if (collectionViewsWork) {
            System.out.println("✓ Collection views work correctly");
        } else {
            System.out.println("✗ Collection views failed");
        }
        recordTestResult("BasicMapOps-CollectionViews", collectionViewsWork, 
                         "Collection views test. Keys size: " + keys.size() + ", Values size: " + values.size() + ", Entries size: " + entries.size());
    }

    public void testQueryOperations() {
        System.out.println("\n=== Testing Query Operations ===");

        // Create a predicate for people older than 30
        System.out.println("Testing simple predicate (age > 30)...");
        Predicate<String, Person> olderThan30 = Predicates.greaterThan("age", 30);
        
        // Execute query
        Collection<Person> olderPeople = personMap.values(olderThan30);
        
        // Verify results
        boolean predicate1Success = olderPeople.size() == 2 && 
                                    olderPeople.stream().allMatch(p -> p.getAge() > 30);
        if (predicate1Success) {
            System.out.println("✓ Simple predicate query works correctly");
        } else {
            System.out.println("✗ Simple predicate query failed. Expected 2 people older than 30");
        }
        recordTestResult("QueryOps-SimplePredicate", predicate1Success, 
                         "Simple predicate query test. Expected 2 people older than 30, Got: " + olderPeople.size());
        
        // Test AND predicate
        System.out.println("Testing compound AND predicate (age > 25 AND active = true)...");
        Predicate<String, Person> olderThan25AndActive = Predicates.and(
            Predicates.greaterThan("age", 25),
            Predicates.equal("active", true)
        );
        
        Collection<Person> filteredPeople = personMap.values(olderThan25AndActive);
        boolean andPredicateSuccess = filteredPeople.size() == 2;
        if (andPredicateSuccess) {
            System.out.println("✓ Compound AND predicate query works correctly");
        } else {
            System.out.println("✗ Compound AND predicate query failed. Expected 2 people, got: " + filteredPeople.size());
        }
        recordTestResult("QueryOps-AndPredicate", andPredicateSuccess, 
                         "Compound AND predicate test. Expected 2 people, Got: " + filteredPeople.size());
        
        // Test OR predicate
        System.out.println("Testing compound OR predicate (age < 25 OR age > 40)...");
        Predicate<String, Person> youngerThan25OrOlderThan40 = Predicates.or(
            Predicates.lessThan("age", 25),
            Predicates.greaterThan("age", 40)
        );
        
        Collection<Person> orFilteredPeople = personMap.values(youngerThan25OrOlderThan40);
        boolean orPredicateSuccess = orFilteredPeople.size() == 2;
        if (orPredicateSuccess) {
            System.out.println("✓ Compound OR predicate query works correctly");
        } else {
            System.out.println("✗ Compound OR predicate query failed. Expected 2 people, got: " + orFilteredPeople.size());
        }
        recordTestResult("QueryOps-OrPredicate", orPredicateSuccess, 
                         "Compound OR predicate test. Expected 2 people, Got: " + orFilteredPeople.size());
        
        // Test NOT predicate
        System.out.println("Testing NOT predicate (NOT active)...");
        Predicate<String, Person> notActive = Predicates.not(Predicates.equal("active", true));
        
        Collection<Person> inactivePeople = personMap.values(notActive);
        boolean notPredicateSuccess = inactivePeople.size() == 1;
        if (notPredicateSuccess) {
            System.out.println("✓ NOT predicate query works correctly");
        } else {
            System.out.println("✗ NOT predicate query failed. Expected 1 person, got: " + inactivePeople.size());
        }
        recordTestResult("QueryOps-NotPredicate", notPredicateSuccess, 
                         "NOT predicate test. Expected 1 person, Got: " + inactivePeople.size());
        
        // Test LIKE predicate
        System.out.println("Testing LIKE predicate (name LIKE %li%)...");
        Predicate<String, Person> nameContainsLi = Predicates.like("name", "%li%");
        
        Collection<Person> liPeople = personMap.values(nameContainsLi);
        boolean likePredicateSuccess = liPeople.size() == 2; // Alice and Charlie
        if (likePredicateSuccess) {
            System.out.println("✓ LIKE predicate query works correctly");
        } else {
            System.out.println("✗ LIKE predicate query failed. Expected 2 people, got: " + liPeople.size());
        }
        recordTestResult("QueryOps-LikePredicate", likePredicateSuccess, 
                         "LIKE predicate test. Expected 2 people, Got: " + liPeople.size());
    }

    public void testEntryProcessor() {
        System.out.println("\n=== Testing Entry Processor ===");

        
        
        System.out.println("Using entry processor to increment all ages...");
        // Use entry processor to update all persons' ages by 1
        Map<String, Object> results = personMap.executeOnEntries(entry -> {
            Person person = entry.getValue();
            person.setAge(person.getAge() + 1);
            entry.setValue(person);
            return "Updated";
        });
        
        // Verify results
        boolean processorReturnedCorrectResults = results.size() == 5 && 
                                                  results.values().stream().allMatch(r -> "Updated".equals(r));
        if (processorReturnedCorrectResults) {
            System.out.println("✓ Entry processor execution returned correct results");
        } else {
            System.out.println("✗ Entry processor execution returned incorrect results");
        }
        recordTestResult("EntryProcessor-ReturnValues", processorReturnedCorrectResults, 
                         "Entry processor returned correct values. Size: " + results.size());
        
        // Verify the ages were updated
        boolean aliceUpdated = personMap.get("p1").getAge() == 33;
        boolean bobUpdated = personMap.get("p2").getAge() == 25;
        boolean agesUpdatedCorrectly = aliceUpdated && bobUpdated;
        
        if (agesUpdatedCorrectly) {
            System.out.println("✓ Entry processor updated ages correctly");
        } else {
            System.out.println("✗ Entry processor failed to update ages correctly");
        }
        recordTestResult("EntryProcessor-Updates", agesUpdatedCorrectly, 
                         "Entry processor updated values correctly. Alice's age: " + personMap.get("p1").getAge() + 
                         ", Bob's age: " + personMap.get("p2").getAge());
        
        // Test entry processor with a filter
        System.out.println("Using entry processor with a filter (only active users)...");
        Predicate<String, Person> activePredicate = Predicates.equal("active", true);
        
        Map<String, Object> filteredResults = personMap.executeOnEntries(entry -> {
            Person person = entry.getValue();
            person.setAge(person.getAge() + 10);
            entry.setValue(person);
            return "Active updated";
        }, activePredicate);
        
        // Verify filtered processing
        boolean filteredProcessingCorrect = filteredResults.size() == 3; // Only active people should be updated
        if (filteredProcessingCorrect) {
            System.out.println("✓ Filtered entry processor affected correct number of entries");
        } else {
            System.out.println("✗ Filtered entry processor affected incorrect number of entries");
        }
        recordTestResult("EntryProcessor-FilteredProcessing", filteredProcessingCorrect, 
                         "Filtered entry processor affected correct number of entries. Size: " + filteredResults.size());
        
        // Verify specific entries
        Person alice = personMap.get("p1");
        Person diana = personMap.get("p4");
        boolean specificEntriesCorrect = alice.getAge() == 43 && diana.getAge() == 42;
        
        if (specificEntriesCorrect) {
            System.out.println("✓ Filtered entry processor updated only active entries");
        } else {
            System.out.println("✗ Filtered entry processor did not update entries correctly");
        }
        recordTestResult("EntryProcessor-FilteredUpdates", specificEntriesCorrect, 
                         "Filtered entry processor updated correct entries. Alice's age: " + alice.getAge() + 
                         ", Diana's age: " + diana.getAge());
    }

    public void testMapWithTTL(int ttlSeconds) throws Exception {
        System.out.println("\n=== Testing Map with TTL (" + ttlSeconds + " seconds) ===");

        
        
        // Create a map with TTL
        IMap<String, String> expiringMap = hazelcastInstance.getMap("expiring-map");
        expiringMap.clear(); // Ensure map is empty before test
        
        // Put an entry with TTL
        System.out.println("Adding entry with " + ttlSeconds + " seconds TTL...");
        expiringMap.put("key1", "value1", ttlSeconds, TimeUnit.SECONDS);
        
        // Verify entry exists
        boolean entryExists = expiringMap.containsKey("key1");
        if (entryExists) {
            System.out.println("✓ Entry with TTL was added successfully");
        } else {
            System.out.println("✗ Failed to add entry with TTL");
        }
        recordTestResult("TTL-AddEntry", entryExists, 
                        "Added entry with TTL. Entry exists: " + entryExists);
        
        // Wait half the TTL time
        System.out.println("Waiting for " + (ttlSeconds/2) + " seconds...");
        Thread.sleep((ttlSeconds * 1000) / 2);
        
        // Entry should still exist
        boolean entryStillExists = expiringMap.containsKey("key1");
        if (entryStillExists) {
            System.out.println("✓ Entry still exists after half TTL period");
        } else {
            System.out.println("✗ Entry expired prematurely");
        }
        recordTestResult("TTL-HalfPeriod", entryStillExists, 
                        "Entry exists after half TTL period: " + entryStillExists);
        
        // Wait until after TTL
        System.out.println("Waiting for remaining TTL period plus 1 second...");
        Thread.sleep((ttlSeconds * 1000) / 2 + 1000);
        
        // Entry should be gone
        boolean entryExpired = !expiringMap.containsKey("key1");
        if (entryExpired) {
            System.out.println("✓ Entry expired after TTL period");
        } else {
            System.out.println("✗ Entry did not expire after TTL period");
        }
        recordTestResult("TTL-Expiration", entryExpired, 
                        "Entry expired after full TTL period: " + entryExpired);
        
        // Test putWithMaxIdle
        System.out.println("Testing put with maxIdle...");
        expiringMap.put("idleKey", "idleValue", 0, TimeUnit.SECONDS, ttlSeconds, TimeUnit.SECONDS);
        
        boolean idleKeyExists = expiringMap.containsKey("idleKey");
        if (idleKeyExists) {
            System.out.println("✓ Entry with maxIdle was added successfully");
        } else {
            System.out.println("✗ Failed to add entry with maxIdle");
        }
        recordTestResult("TTL-AddWithMaxIdle", idleKeyExists, 
                        "Added entry with maxIdle. Entry exists: " + idleKeyExists);
        
        // Wait for idle expiration
        System.out.println("Waiting for idle expiration...");
        Thread.sleep(ttlSeconds * 1000 + 1000);
        
        boolean idleExpired = !expiringMap.containsKey("idleKey");
        if (idleExpired) {
            System.out.println("✓ Entry with maxIdle expired correctly");
        } else {
            System.out.println("✗ Entry with maxIdle did not expire correctly");
        }
        recordTestResult("TTL-MaxIdleExpiration", idleExpired, 
                        "Entry with maxIdle expired correctly: " + idleExpired);
    }

    public void testConcurrentOperations() throws Exception {
        System.out.println("\n=== Testing Concurrent Map Operations ===");

        
        
        // Create a map for testing concurrency
        IMap<String, Integer> counterMap = hazelcastInstance.getMap("counter-map");
        counterMap.clear();
        counterMap.put("counter", 0);
        
        // Number of concurrent increments
        int numIncrements = 100;
        System.out.println("Performing " + numIncrements + " increments with optimistic locking...");
        
        // Use optimistic locking
        for (int i = 0; i < numIncrements; i++) {
            boolean success = false;
            while (!success) {
                Integer currentValue = counterMap.get("counter");
                Integer newValue = currentValue + 1;
                success = counterMap.replace("counter", currentValue, newValue);
            }
        }
        
        // Verify final count
        int finalCount = counterMap.get("counter");
        boolean incrementsSucceeded = finalCount == numIncrements;
        if (incrementsSucceeded) {
            System.out.println("✓ Optimistic concurrency control worked correctly");
        } else {
            System.out.println("✗ Optimistic concurrency control failed. Expected: " + numIncrements + ", Actual: " + finalCount);
        }
        recordTestResult("Concurrency-OptimisticLocking", incrementsSucceeded, 
                        "Optimistic concurrency control test. Expected: " + numIncrements + ", Actual: " + finalCount);
        
        // Test with multiple threads
        System.out.println("Testing concurrent access with multiple threads...");
        counterMap.put("counter", 0);
        
        int threadCount = 5;
        int incrementsPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int i = 0; i < incrementsPerThread; i++) {
                        boolean success = false;
                        while (!success) {
                            Integer currentValue = counterMap.get("counter");
                            Integer newValue = currentValue + 1;
                            success = counterMap.replace("counter", currentValue, newValue);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads at once
        startLatch.countDown();
        
        // Wait for all threads to complete
        finishLatch.await(30, TimeUnit.SECONDS);
        
        // Verify final count
        int multiThreadedFinalCount = counterMap.get("counter");
        int expectedMultiThreadedCount = threadCount * incrementsPerThread;
        boolean multiThreadSucceeded = multiThreadedFinalCount == expectedMultiThreadedCount;
        
        if (multiThreadSucceeded) {
            System.out.println("✓ Multi-threaded concurrency control worked correctly");
        } else {
            System.out.println("✗ Multi-threaded concurrency control failed. Expected: " + expectedMultiThreadedCount + ", Actual: " + multiThreadedFinalCount);
        }
        recordTestResult("Concurrency-MultiThreaded", multiThreadSucceeded, 
                        "Multi-threaded concurrency test. Expected: " + expectedMultiThreadedCount + ", Actual: " + multiThreadedFinalCount);
    }

    public void testMapListeners() throws Exception {
        System.out.println("\n=== Testing Map Listeners ===");

        
        
        // Track events
        final CountDownLatch addLatch = new CountDownLatch(2);
        final CountDownLatch updateLatch = new CountDownLatch(1);
        final CountDownLatch removeLatch = new CountDownLatch(1);
        
        System.out.println("Adding entry listeners...");
        // Add entry listener for different event types
        UUID listenerId = personMap.addEntryListener(
            (EntryAddedListener<String, Person>) event -> {
                System.out.println("Entry added: " + event.getKey() + " -> " + event.getValue().getName());
                addLatch.countDown();
            }, true);
            
        personMap.addEntryListener(
            (EntryUpdatedListener<String, Person>) event -> {
                System.out.println("Entry updated: " + event.getKey() + " -> " + event.getValue().getName());
                updateLatch.countDown();
            }, true);
            
        personMap.addEntryListener(
            (EntryRemovedListener<String, Person>) event -> {
                System.out.println("Entry removed: " + event.getKey());
                removeLatch.countDown();
            }, true);
        
        // Perform operations
        System.out.println("Performing operations to trigger events...");
        personMap.put("p7", new Person("Grace", 35, true));
        personMap.put("p8", new Person("Henry", 45, false));
        personMap.put("p1", new Person("Alice", 33, true)); // Update
        personMap.remove("p8"); // Remove
        
        // Wait for events
        boolean addEvents = addLatch.await(5, TimeUnit.SECONDS);
        boolean updateEvents = updateLatch.await(5, TimeUnit.SECONDS);
        boolean removeEvents = removeLatch.await(5, TimeUnit.SECONDS);
        
        // Verify events
        boolean allEventsReceived = addEvents && updateEvents && removeEvents;
        if (allEventsReceived) {
            System.out.println("✓ All listener events were received correctly");
        } else {
            System.out.println("✗ Not all listener events were received");
        }
        recordTestResult("Listeners-EventNotification", allEventsReceived, 
                        "Listener event notification test. Add events: " + addEvents + 
                        ", Update events: " + updateEvents + ", Remove events: " + removeEvents);
        
        // Test listener removal
        System.out.println("Testing listener removal...");
        boolean removed = personMap.removeEntryListener(listenerId);
        
        if (removed) {
            System.out.println("✓ Listener removal worked correctly");
        } else {
            System.out.println("✗ Listener removal failed");
        }
        recordTestResult("Listeners-RemoveListener", removed, 
                        "Listener removal test. Successfully removed: " + removed);
    }

    public void testAsyncOperations() throws Exception {
        System.out.println("\n=== Testing Async Map Operations ===");

        
        
        // Test async put
        System.out.println("Testing async put...");
        personMap.setAsync("p9", new Person("Ian", 28, true)).toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        boolean asyncPutWorked = personMap.containsKey("p9");
        if (asyncPutWorked) {
            System.out.println("✓ Async put operation worked correctly");
        } else {
            System.out.println("✗ Async put operation failed");
        }
        recordTestResult("Async-Put", asyncPutWorked, 
                        "Async put operation test. Entry exists: " + asyncPutWorked);
        
        // Test async get
        System.out.println("Testing async get...");
        Person asyncPerson = personMap.getAsync("p9").toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        boolean asyncGetWorked = asyncPerson != null && "Ian".equals(asyncPerson.getName());
        if (asyncGetWorked) {
            System.out.println("✓ Async get operation worked correctly");
        } else {
            System.out.println("✗ Async get operation failed");
        }
        recordTestResult("Async-Get", asyncGetWorked, 
                        "Async get operation test. Retrieved person: " + (asyncPerson != null ? asyncPerson.getName() : "null"));
        
        // Test async remove
        System.out.println("Testing async remove...");
        Person removedPerson = personMap.removeAsync("p9").toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        boolean asyncRemoveWorked = removedPerson != null && "Ian".equals(removedPerson.getName()) && !personMap.containsKey("p9");
        if (asyncRemoveWorked) {
            System.out.println("✓ Async remove operation worked correctly");
        } else {
            System.out.println("✗ Async remove operation failed");
        }
        recordTestResult("Async-Remove", asyncRemoveWorked, 
                        "Async remove operation test. Removed person: " + (removedPerson != null ? removedPerson.getName() : "null") + 
                        ", Entry exists: " + personMap.containsKey("p9"));
    }

    public void testLockOperations() throws Exception {
        System.out.println("\n=== Testing Map Lock Operations ===");

        
        
        // Test locking
        System.out.println("Testing lock operation...");
        personMap.lock("p1");
        
        try {
            Person lockedPerson = personMap.get("p1");
            // Update while locked (should be safe)
            personMap.put("p1", new Person("Alice", lockedPerson.getAge() + 1, lockedPerson.isActive()));
            
            boolean lockWorked = personMap.get("p1").getAge() == lockedPerson.getAge() + 1;
            if (lockWorked) {
                System.out.println("✓ Lock operation worked correctly");
            } else {
                System.out.println("✗ Lock operation failed");
            }
            recordTestResult("Locks-BasicLock", lockWorked, 
                            "Basic lock operation test. Updated age: " + personMap.get("p1").getAge());
        } finally {
            personMap.unlock("p1");
        }
        
        // Test tryLock
        System.out.println("Testing tryLock operation...");
        boolean locked = personMap.tryLock("p2", 1, TimeUnit.SECONDS);
        
        if (locked) {
            try {
                System.out.println("✓ TryLock operation worked correctly");
                recordTestResult("Locks-TryLock", true, "TryLock operation test succeeded");
            } finally {
                personMap.unlock("p2");
            }
        } else {
            System.out.println("✗ TryLock operation failed");
            recordTestResult("Locks-TryLock", false, "TryLock operation test failed");
        }
    }

    public void testEvictionOperations() throws Exception {
        System.out.println("\n=== Testing Map Eviction Operations ===");

        
        
        // Note: Eviction configuration is usually set in the Hazelcast configuration
        // Here we'll test basic eviction-related operations
        
        IMap<String, String> evictionMap = hazelcastInstance.getMap("eviction-test-map");
        evictionMap.clear();
        
        // Add some entries
        for (int i = 0; i < 10; i++) {
            evictionMap.put("key" + i, "value" + i);
        }
        
        // Test evict
        System.out.println("Testing evict operation...");
        boolean evicted = evictionMap.evict("key0");
        
        if (evicted && !evictionMap.containsKey("key0")) {
            System.out.println("✓ Evict operation worked correctly");
        } else {
            System.out.println("✗ Evict operation failed");
        }
        recordTestResult("Eviction-SingleEvict", evicted && !evictionMap.containsKey("key0"), 
                        "Single entry eviction test. Evicted: " + evicted + ", Entry exists: " + evictionMap.containsKey("key0"));
        
        // Test evictAll
        System.out.println("Testing evictAll operation...");
        evictionMap.evictAll();
        
        boolean allEvicted = evictionMap.size() == 0;
        if (allEvicted) {
            System.out.println("✓ EvictAll operation worked correctly");
        } else {
            System.out.println("✗ EvictAll operation failed");
        }
        recordTestResult("Eviction-EvictAll", allEvicted, 
                        "EvictAll operation test. Map size after eviction: " + evictionMap.size());
    }

    public void testBulkOperations() throws Exception {
        System.out.println("\n=== Testing Map Bulk Operations ===");

        
        
        // Clear and prepare map
        personMap.clear();
        
        // Test putAll
        System.out.println("Testing putAll operation...");
        Map<String, Person> batchMap = new HashMap<>();
        batchMap.put("p1", new Person("Alice", 32, true));
        batchMap.put("p2", new Person("Bob", 24, true));
        batchMap.put("p3", new Person("Charlie", 29, true));
        
        personMap.putAll(batchMap);
        
        boolean putAllWorked = personMap.size() == 3;
        if (putAllWorked) {
            System.out.println("✓ PutAll operation worked correctly");
        } else {
            System.out.println("✗ PutAll operation failed");
        }
        recordTestResult("Bulk-PutAll", putAllWorked, 
                        "PutAll operation test. Map size after operation: " + personMap.size());
        
        // Test getAll
        System.out.println("Testing getAll operation...");
        Set<String> keys = personMap.keySet();
        Map<String, Person> allPeople = personMap.getAll(keys);
        
        boolean getAllWorked = allPeople.size() == 3 && 
                               allPeople.containsKey("p1") && 
                               allPeople.containsKey("p2") && 
                               allPeople.containsKey("p3");
        if (getAllWorked) {
            System.out.println("✓ GetAll operation worked correctly");
        } else {
            System.out.println("✗ GetAll operation failed");
        }
        recordTestResult("Bulk-GetAll", getAllWorked, 
                        "GetAll operation test. Retrieved entries: " + allPeople.size());
    }
}
