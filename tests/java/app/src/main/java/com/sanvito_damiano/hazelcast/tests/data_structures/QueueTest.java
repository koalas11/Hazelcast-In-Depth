package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
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
        
        if (offered1 && offered2 && offered3 && queue.size() == 3) {
            System.out.println("✓ Offer operation works correctly");
        } else {
            System.out.println("✗ Offer operation failed");
        }
        
        // Test peek
        System.out.println("Testing peek operation...");
        String peeked = queue.peek();
        if ("item1".equals(peeked) && queue.size() == 3) {
            System.out.println("✓ Peek operation works correctly");
        } else {
            System.out.println("✗ Peek operation failed");
        }
        
        // Test element (same as peek but throws exception if empty)
        System.out.println("Testing element operation...");
        String element = queue.element();
        if ("item1".equals(element) && queue.size() == 3) {
            System.out.println("✓ Element operation works correctly");
        } else {
            System.out.println("✗ Element operation failed");
        }
        
        // Test poll
        System.out.println("Testing poll operation...");
        String polled = queue.poll();
        if ("item1".equals(polled) && queue.size() == 2) {
            System.out.println("✓ Poll operation works correctly");
        } else {
            System.out.println("✗ Poll operation failed");
        }
        
        // Test remove
        System.out.println("Testing remove operation...");
        String removed = queue.remove();
        if ("item2".equals(removed) && queue.size() == 1) {
            System.out.println("✓ Remove operation works correctly");
        } else {
            System.out.println("✗ Remove operation failed");
        }
        
        // Test contains
        System.out.println("Testing contains operation...");
        boolean contains = queue.contains("item3");
        if (contains) {
            System.out.println("✓ Contains operation works correctly");
        } else {
            System.out.println("✗ Contains operation failed");
        }
        
        // Test clear
        System.out.println("Testing clear operation...");
        queue.clear();
        if (queue.isEmpty() && queue.size() == 0) {
            System.out.println("✓ Clear operation works correctly");
        } else {
            System.out.println("✗ Clear operation failed");
        }
    }

    public void testBlockingOperations() throws Exception {
        System.out.println("\n=== Testing Blocking Queue Operations ===");
        
        // Clear the queue
        queue.clear();
        
        // Test put operation
        System.out.println("Testing put operation...");
        queue.put("item1");
        queue.put("item2");
        
        if (queue.size() == 2) {
            System.out.println("✓ Put operation works correctly");
        } else {
            System.out.println("✗ Put operation failed");
        }
        
        // Test take operation
        System.out.println("Testing take operation...");
        String taken = queue.take();
        
        if ("item1".equals(taken) && queue.size() == 1) {
            System.out.println("✓ Take operation works correctly");
        } else {
            System.out.println("✗ Take operation failed");
        }
        
        // Test poll with timeout
        System.out.println("Testing poll with timeout...");
        String polled = queue.poll(1, TimeUnit.SECONDS);
        
        if ("item2".equals(polled) && queue.isEmpty()) {
            System.out.println("✓ Poll with timeout works correctly");
        } else {
            System.out.println("✗ Poll with timeout failed");
        }
        
        // Test poll with timeout on empty queue
        System.out.println("Testing poll with timeout on empty queue...");
        long startTime = System.currentTimeMillis();
        String shouldBeNull = queue.poll(1, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        if (shouldBeNull == null && duration >= 1000) {
            System.out.println("✓ Poll with timeout on empty queue works correctly");
        } else {
            System.out.println("✗ Poll with timeout on empty queue failed");
        }
    }

    public void testBulkOperations() {
        System.out.println("\n=== Testing Queue Bulk Operations ===");
        
        // Clear the queue
        queue.clear();
        
        // Test addAll
        System.out.println("Testing addAll operation...");
        Collection<String> toAdd = Arrays.asList("item1", "item2", "item3");
        boolean added = queue.addAll(toAdd);
        
        if (added && queue.size() == 3) {
            System.out.println("✓ AddAll operation works correctly");
        } else {
            System.out.println("✗ AddAll operation failed");
        }
        
        // Test containsAll
        System.out.println("Testing containsAll operation...");
        Collection<String> toCheck = Arrays.asList("item1", "item3");
        boolean containsAll = queue.containsAll(toCheck);
        
        if (containsAll) {
            System.out.println("✓ ContainsAll operation works correctly");
        } else {
            System.out.println("✗ ContainsAll operation failed");
        }
        
        // Test removeAll
        System.out.println("Testing removeAll operation...");
        Collection<String> toRemove = Arrays.asList("item1", "item3");
        boolean removed = queue.removeAll(toRemove);
        
        if (removed && queue.size() == 1 && queue.contains("item2")) {
            System.out.println("✓ RemoveAll operation works correctly");
        } else {
            System.out.println("✗ RemoveAll operation failed");
        }
        
        // Test retainAll
        System.out.println("Testing retainAll operation...");
        queue.clear();
        queue.addAll(Arrays.asList("item1", "item2", "item3"));
        Collection<String> toRetain = Arrays.asList("item2");
        boolean retained = queue.retainAll(toRetain);
        
        if (retained && queue.size() == 1 && queue.contains("item2")) {
            System.out.println("✓ RetainAll operation works correctly");
        } else {
            System.out.println("✗ RetainAll operation failed");
        }
    }

    public void testDrainOperations() {
        System.out.println("\n=== Testing Queue Drain Operations ===");
        
        // Clear the queue
        queue.clear();
        queue.addAll(Arrays.asList("item1", "item2", "item3", "item4", "item5"));
        
        // Test drainTo operation
        System.out.println("Testing drainTo operation...");
        Collection<String> drained = new ArrayList<>();
        int drainCount = queue.drainTo(drained);
        
        if (drainCount == 5 && drained.size() == 5 && queue.isEmpty()) {
            System.out.println("✓ DrainTo operation works correctly");
        } else {
            System.out.println("✗ DrainTo operation failed");
        }
        
        // Test drainTo with maxElements
        System.out.println("Testing drainTo with maxElements...");
        queue.addAll(Arrays.asList("item1", "item2", "item3", "item4", "item5"));
        Collection<String> drainedPartial = new ArrayList<>();
        int drainCountPartial = queue.drainTo(drainedPartial, 3);
        
        if (drainCountPartial == 3 && drainedPartial.size() == 3 && queue.size() == 2) {
            System.out.println("✓ DrainTo with maxElements works correctly");
        } else {
            System.out.println("✗ DrainTo with maxElements failed");
        }
    }

    public void testListeners() throws Exception {
        System.out.println("\n=== Testing Queue Listeners ===");
        
        // Clear the queue
        queue.clear();
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] itemAdded = new boolean[1];
        
        // Add an item listener
        System.out.println("Adding item listener...");
        queue.addItemListener(new ItemListener<String>() {
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
        
        if (received && itemAdded[0]) {
            System.out.println("✓ Queue listener works correctly");
        } else {
            System.out.println("✗ Queue listener failed or timed out");
        }
    }

    public void testProducerConsumerPattern() throws Exception {
        System.out.println("\n=== Testing Producer-Consumer Pattern ===");
        
        // Clear the queue
        queue.clear();
        
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
        
        if (completed && success[0]) {
            System.out.println("✓ Producer-Consumer pattern works correctly");
        } else {
            System.out.println("✗ Producer-Consumer pattern failed or timed out");
        }
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
        
        try {
            for (added = 0; added < maxToAdd; added++) {
                boundedQueue.add("capacity-item" + added);
            }
            System.out.println("✓ Added " + added + " items to queue");
        } catch (IllegalStateException e) {
            System.out.println("✓ Queue reached capacity after " + added + " items as expected");
        }
        
        // Clean up
        boundedQueue.clear();
    }
}
