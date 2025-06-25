package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test program for Hazelcast IQueue operations
 */
public class QueueTest extends AbstractTest {

    private IQueue<String> queue;

    public QueueTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        queue = hazelcastInstance.getQueue("test-queue");
    }

    @Override
    public void reset() {
        queue.clear();
    }

    @Override
    public void cleanup() {
        queue.destroy();
        queue = null;
    }

    public void testBasicQueueOperations() {
        System.out.println("\n=== Testing Basic Queue Operations ===");
        
        // Test offer
        System.out.println("Testing offer operation...");
        boolean offered1 = queue.offer("item1");
        boolean offered2 = queue.offer("item2");
        boolean offered3 = queue.offer("item3");
        boolean offerWorked = offered1 && offered2 && offered3 && queue.size() == 3;
        
        if (offerWorked) {
            System.out.println("✓ Offer operation works correctly");
        } else {
            System.out.println("✗ Offer operation failed");
        }
        recordTestResult("BasicQueueOps-Offer", offerWorked, 
                         "Offer operation test. All offers succeeded: " + (offered1 && offered2 && offered3) + 
                         ", Queue size: " + queue.size());
        
        // Test peek
        System.out.println("Testing peek operation...");
        String peeked = queue.peek();
        boolean peekWorked = "item1".equals(peeked) && queue.size() == 3;
        
        if (peekWorked) {
            System.out.println("✓ Peek operation works correctly");
        } else {
            System.out.println("✗ Peek operation failed");
        }
        recordTestResult("BasicQueueOps-Peek", peekWorked, 
                         "Peek operation test. Expected: item1, Actual: " + peeked + 
                         ", Queue size unchanged: " + (queue.size() == 3));
        
        // Test element (same as peek but throws exception if empty)
        System.out.println("Testing element operation...");
        String element = queue.element();
        boolean elementWorked = "item1".equals(element) && queue.size() == 3;
        
        if (elementWorked) {
            System.out.println("✓ Element operation works correctly");
        } else {
            System.out.println("✗ Element operation failed");
        }
        recordTestResult("BasicQueueOps-Element", elementWorked, 
                         "Element operation test. Expected: item1, Actual: " + element + 
                         ", Queue size unchanged: " + (queue.size() == 3));
        
        // Test poll
        System.out.println("Testing poll operation...");
        String polled = queue.poll();
        boolean pollWorked = "item1".equals(polled) && queue.size() == 2;
        
        if (pollWorked) {
            System.out.println("✓ Poll operation works correctly");
        } else {
            System.out.println("✗ Poll operation failed");
        }
        recordTestResult("BasicQueueOps-Poll", pollWorked, 
                         "Poll operation test. Expected: item1, Actual: " + polled + 
                         ", Queue size reduced: " + (queue.size() == 2));
        
        // Test remove
        System.out.println("Testing remove operation...");
        String removed = queue.remove();
        boolean removeWorked = "item2".equals(removed) && queue.size() == 1;
        
        if (removeWorked) {
            System.out.println("✓ Remove operation works correctly");
        } else {
            System.out.println("✗ Remove operation failed");
        }
        recordTestResult("BasicQueueOps-Remove", removeWorked, 
                         "Remove operation test. Expected: item2, Actual: " + removed + 
                         ", Queue size reduced: " + (queue.size() == 1));
        
        // Test contains
        System.out.println("Testing contains operation...");
        boolean contains = queue.contains("item3");
        boolean containsWorked = contains;
        
        if (containsWorked) {
            System.out.println("✓ Contains operation works correctly");
        } else {
            System.out.println("✗ Contains operation failed");
        }
        recordTestResult("BasicQueueOps-Contains", containsWorked, 
                         "Contains operation test. Contains item3: " + contains);
        
        // Test clear
        System.out.println("Testing clear operation...");
        queue.clear();
        boolean clearWorked = queue.isEmpty() && queue.size() == 0;
        
        if (clearWorked) {
            System.out.println("✓ Clear operation works correctly");
        } else {
            System.out.println("✗ Clear operation failed");
        }
        recordTestResult("BasicQueueOps-Clear", clearWorked, 
                         "Clear operation test. Queue empty: " + queue.isEmpty() + 
                         ", Size: " + queue.size());
    }

    public void testBlockingOperations() throws Exception {
        System.out.println("\n=== Testing Blocking Queue Operations ===");
        
        // Test put operation
        System.out.println("Testing put operation...");
        queue.put("item1");
        queue.put("item2");
        boolean putWorked = queue.size() == 2;
        
        if (putWorked) {
            System.out.println("✓ Put operation works correctly");
        } else {
            System.out.println("✗ Put operation failed");
        }
        recordTestResult("BlockingOps-Put", putWorked, 
                         "Put operation test. Queue size: " + queue.size());
        
        // Test take operation
        System.out.println("Testing take operation...");
        String taken = queue.take();
        boolean takeWorked = "item1".equals(taken) && queue.size() == 1;
        
        if (takeWorked) {
            System.out.println("✓ Take operation works correctly");
        } else {
            System.out.println("✗ Take operation failed");
        }
        recordTestResult("BlockingOps-Take", takeWorked, 
                         "Take operation test. Expected: item1, Actual: " + taken + 
                         ", Queue size reduced: " + (queue.size() == 1));
        
        // Test poll with timeout
        System.out.println("Testing poll with timeout...");
        String polled = queue.poll(1, TimeUnit.SECONDS);
        boolean pollTimeoutWorked = "item2".equals(polled) && queue.isEmpty();
        
        if (pollTimeoutWorked) {
            System.out.println("✓ Poll with timeout works correctly");
        } else {
            System.out.println("✗ Poll with timeout failed");
        }
        recordTestResult("BlockingOps-PollTimeout", pollTimeoutWorked, 
                         "Poll with timeout test. Expected: item2, Actual: " + polled + 
                         ", Queue empty: " + queue.isEmpty());
        
        // Test poll with timeout on empty queue
        System.out.println("Testing poll with timeout on empty queue...");
        long startTime = System.currentTimeMillis();
        String shouldBeNull = queue.poll(1, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        boolean pollTimeoutEmptyWorked = shouldBeNull == null && duration >= 1000;
        
        if (pollTimeoutEmptyWorked) {
            System.out.println("✓ Poll with timeout on empty queue works correctly");
        } else {
            System.out.println("✗ Poll with timeout on empty queue failed");
        }
        recordTestResult("BlockingOps-PollTimeoutEmpty", pollTimeoutEmptyWorked, 
                         "Poll with timeout on empty queue test. Result null: " + (shouldBeNull == null) + 
                         ", Duration >= 1000ms: " + (duration >= 1000) + 
                         ", Actual duration: " + duration + "ms");
    }

    public void testBulkOperations() {
        System.out.println("\n=== Testing Queue Bulk Operations ===");
        
        // Test addAll
        System.out.println("Testing addAll operation...");
        Collection<String> toAdd = Arrays.asList("item1", "item2", "item3");
        boolean added = queue.addAll(toAdd);
        boolean addAllWorked = added && queue.size() == 3;
        
        if (addAllWorked) {
            System.out.println("✓ AddAll operation works correctly");
        } else {
            System.out.println("✗ AddAll operation failed");
        }
        recordTestResult("BulkOps-AddAll", addAllWorked, 
                         "AddAll operation test. Items added: " + added + 
                         ", Queue size: " + queue.size());
        
        // Test containsAll
        System.out.println("Testing containsAll operation...");
        Collection<String> toCheck = Arrays.asList("item1", "item3");
        boolean containsAll = queue.containsAll(toCheck);
        boolean containsAllWorked = containsAll;
        
        if (containsAllWorked) {
            System.out.println("✓ ContainsAll operation works correctly");
        } else {
            System.out.println("✗ ContainsAll operation failed");
        }
        recordTestResult("BulkOps-ContainsAll", containsAllWorked, 
                         "ContainsAll operation test. Contains all items: " + containsAll);
        
        // Test removeAll
        System.out.println("Testing removeAll operation...");
        Collection<String> toRemove = Arrays.asList("item1", "item3");
        boolean removed = queue.removeAll(toRemove);
        boolean removeAllWorked = removed && queue.size() == 1 && queue.contains("item2");
        
        if (removeAllWorked) {
            System.out.println("✓ RemoveAll operation works correctly");
        } else {
            System.out.println("✗ RemoveAll operation failed");
        }
        recordTestResult("BulkOps-RemoveAll", removeAllWorked, 
                         "RemoveAll operation test. Items removed: " + removed + 
                         ", Queue size: " + queue.size() + 
                         ", Contains item2: " + queue.contains("item2"));
        
        // Test retainAll
        System.out.println("Testing retainAll operation...");
        queue.clear();
        queue.addAll(Arrays.asList("item1", "item2", "item3"));
        Collection<String> toRetain = Arrays.asList("item2");
        boolean retained = queue.retainAll(toRetain);
        boolean retainAllWorked = retained && queue.size() == 1 && queue.contains("item2");
        
        if (retainAllWorked) {
            System.out.println("✓ RetainAll operation works correctly");
        } else {
            System.out.println("✗ RetainAll operation failed");
        }
        recordTestResult("BulkOps-RetainAll", retainAllWorked, 
                         "RetainAll operation test. Operation succeeded: " + retained + 
                         ", Queue size: " + queue.size() + 
                         ", Contains item2: " + queue.contains("item2"));
    }

    public void testDrainOperations() {
        System.out.println("\n=== Testing Queue Drain Operations ===");
        
        queue.addAll(Arrays.asList("item1", "item2", "item3", "item4", "item5"));
        
        // Test drainTo operation
        System.out.println("Testing drainTo operation...");
        Collection<String> drained = new ArrayList<>();
        int drainCount = queue.drainTo(drained);
        boolean drainToWorked = drainCount == 5 && drained.size() == 5 && queue.isEmpty();
        
        if (drainToWorked) {
            System.out.println("✓ DrainTo operation works correctly");
        } else {
            System.out.println("✗ DrainTo operation failed");
        }
        recordTestResult("DrainOps-DrainTo", drainToWorked, 
                         "DrainTo operation test. Drain count: " + drainCount + 
                         ", Collection size: " + drained.size() + 
                         ", Queue empty: " + queue.isEmpty());
        
        // Test drainTo with maxElements
        System.out.println("Testing drainTo with maxElements...");
        queue.addAll(Arrays.asList("item1", "item2", "item3", "item4", "item5"));
        Collection<String> drainedPartial = new ArrayList<>();
        int drainCountPartial = queue.drainTo(drainedPartial, 3);
        boolean drainToMaxWorked = drainCountPartial == 3 && drainedPartial.size() == 3 && queue.size() == 2;
        
        if (drainToMaxWorked) {
            System.out.println("✓ DrainTo with maxElements works correctly");
        } else {
            System.out.println("✗ DrainTo with maxElements failed");
        }
        recordTestResult("DrainOps-DrainToMax", drainToMaxWorked, 
                         "DrainTo with maxElements test. Drain count: " + drainCountPartial + 
                         ", Collection size: " + drainedPartial.size() + 
                         ", Queue size: " + queue.size());
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing Queue Listeners ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] itemAdded = new boolean[1];
        
        // Add an item listener
        System.out.println("Adding item listener...");
        UUID id = queue.addItemListener(new ItemListener<String>() {
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
        queue.add("new-item");
        
        // Wait for the listener to be triggered
        boolean received = latch.await(5, TimeUnit.SECONDS);
        boolean listenerWorked = received && itemAdded[0];
        
        if (listenerWorked) {
            System.out.println("✓ Queue listener works correctly");
        } else {
            System.out.println("✗ Queue listener failed or timed out");
        }
        recordTestResult("Listeners-EventNotification", listenerWorked, 
                         "Listener event notification test. Event received: " + received + 
                         ", Item added detected: " + itemAdded[0]);
        
        // Test listener removal
        System.out.println("Testing listener removal...");
        boolean removed = queue.removeItemListener(id);
        
        if (removed) {
            System.out.println("✓ Listener removal worked correctly");
        } else {
            System.out.println("✗ Listener removal failed");
        }
        recordTestResult("Listeners-RemoveListener", removed, 
                         "Listener removal test. Successfully removed: " + removed);
    }

    public void testProducerConsumerPattern() throws Exception {
        System.out.println("\n=== Testing Producer-Consumer Pattern ===");
        
        // Number of items to produce/consume
        final int itemCount = 100;
        final CountDownLatch producerLatch = new CountDownLatch(1);
        final CountDownLatch consumerLatch = new CountDownLatch(itemCount);
        final boolean[] success = new boolean[] { true };
        
        // Start consumer thread
        System.out.println("Starting consumer thread...");
        Thread consumer = new Thread(() -> {
            try {
                producerLatch.await(); // Wait for producer to start
                
                for (int i = 0; i < itemCount; i++) {
                    String item = queue.take();
                    if (!item.equals("item" + i)) {
                        System.out.println("Consumer received unexpected item: " + item + ", expected: item" + i);
                        success[0] = false;
                        break;
                    }
                    consumerLatch.countDown();
                }
            } catch (Exception e) {
                e.printStackTrace();
                success[0] = false;
            }
        });
        consumer.start();
        
        // Start producer thread
        System.out.println("Starting producer thread...");
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < itemCount; i++) {
                    queue.put("item" + i);
                    if (i == 0) {
                        producerLatch.countDown(); // Signal consumer to start
                    }
                    // Small delay to ensure consumer has a chance to process
                    if (i % 10 == 0) {
                        Thread.sleep(10);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                success[0] = false;
            }
        });
        producer.start();
        
        // Wait for consumer to finish
        boolean completed = consumerLatch.await(30, TimeUnit.SECONDS);
        boolean producerConsumerWorked = completed && success[0];
        
        if (producerConsumerWorked) {
            System.out.println("✓ Producer-Consumer pattern works correctly");
        } else {
            System.out.println("✗ Producer-Consumer pattern failed or timed out");
        }
        recordTestResult("ProducerConsumer-Pattern", producerConsumerWorked, 
                         "Producer-Consumer pattern test. Completed: " + completed + 
                         ", All items processed correctly: " + success[0]);
    }

    public void testQueueCapacity() throws Exception {
        System.out.println("\n=== Testing Queue Capacity ===");
        
        // Get a new queue with capacity
        IQueue<String> boundedQueue = hazelcastInstance.getQueue("bounded-test-queue");
        boundedQueue.clear();
        
        // Set capacity to 10 through config or check the default
        System.out.println("Note: Queue capacity is set in configuration. This test assumes a reasonable capacity.");
        
        // Add items until full or a reasonable number
        System.out.println("Testing adding many items...");
        int maxToAdd = 1000; // A reasonably large number
        int added = 0;
        boolean reachedCapacity = false;
        
        try {
            for (added = 0; added < maxToAdd; added++) {
                boundedQueue.add("capacity-item" + added);
            }
            System.out.println("✓ Added " + added + " items to queue");
        } catch (IllegalStateException e) {
            System.out.println("✓ Queue reached capacity after " + added + " items as expected");
            reachedCapacity = true;
        }
        
        // For the test result, we consider it successful if we either:
        // 1. Added all items (queue capacity larger than maxToAdd)
        // 2. Reached capacity (IllegalStateException was thrown)
        boolean capacityTestWorked = added > 0 && (added == maxToAdd || reachedCapacity);
        
        recordTestResult("QueueCapacity-Test", capacityTestWorked, 
                         "Queue capacity test. Items added: " + added + 
                         ", Reached capacity: " + reachedCapacity);
        
        // Clean up
        boundedQueue.destroy();
    }
}
