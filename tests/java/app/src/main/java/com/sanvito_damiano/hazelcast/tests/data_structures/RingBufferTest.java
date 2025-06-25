package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;
import com.hazelcast.ringbuffer.OverflowPolicy;
import com.hazelcast.ringbuffer.ReadResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test program for Hazelcast Ringbuffer functionality
 */
public class RingBufferTest extends AbstractTest {

    private Ringbuffer<String> ringbuffer;

    public RingBufferTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        ringbuffer = hazelcastInstance.getRingbuffer("test-ringbuffer");
    }

    @Override
    public void reset() {
        cleanup();
        setup();
    }

    @Override
    public void cleanup() {
        ringbuffer.destroy();
        ringbuffer = null;
    }

    public void testBasicOperations() throws Exception {
        System.out.println("\n=== Testing Basic Ringbuffer Operations ===");
        
        // Test size and capacity
        System.out.println("Testing initial size and capacity...");
        long size = ringbuffer.size();
        long capacity = ringbuffer.capacity();
        
        boolean initialSizeCapacityCorrect = size == 0 && capacity > 0;
        if (initialSizeCapacityCorrect) {
            System.out.println("✓ Initial size (0) and capacity (" + capacity + ") are correct");
        } else {
            System.out.println("✗ Unexpected initial size or capacity. Size: " + size + ", Capacity: " + capacity);
        }
        recordTestResult("BasicOps-InitialState", initialSizeCapacityCorrect, 
                         "Initial size and capacity check. Size: " + size + 
                         ", Capacity: " + capacity);
        
        // Test add
        System.out.println("Testing add operation...");
        long sequence1 = ringbuffer.add("item1");
        long sequence2 = ringbuffer.add("item2");
        long sequence3 = ringbuffer.add("item3");
        
        boolean addWorked = sequence1 == 0 && sequence2 == 1 && sequence3 == 2 && ringbuffer.size() == 3;
        if (addWorked) {
            System.out.println("✓ Add operation works correctly");
        } else {
            System.out.println("✗ Add operation failed");
        }
        recordTestResult("BasicOps-Add", addWorked, 
                         "Add operation test. Sequences: " + sequence1 + "," + 
                         sequence2 + "," + sequence3 + ", Size: " + ringbuffer.size());
        
        // Test readOne
        System.out.println("Testing readOne operation...");
        String item1 = ringbuffer.readOne(sequence1);
        String item3 = ringbuffer.readOne(sequence3);
        
        boolean readOneWorked = "item1".equals(item1) && "item3".equals(item3);
        if (readOneWorked) {
            System.out.println("✓ ReadOne operation works correctly");
        } else {
            System.out.println("✗ ReadOne operation failed");
        }
        recordTestResult("BasicOps-ReadOne", readOneWorked, 
                         "ReadOne operation test. Item1: " + item1 + 
                         ", Item3: " + item3);
        
        // Test headSequence and tailSequence
        System.out.println("Testing headSequence and tailSequence...");
        long headSequence = ringbuffer.headSequence();
        long tailSequence = ringbuffer.tailSequence();
        
        boolean sequencesCorrect = headSequence == 0 && tailSequence == 2;
        if (sequencesCorrect) {
            System.out.println("✓ HeadSequence and tailSequence are correct");
        } else {
            System.out.println("✗ Unexpected head or tail sequence. Head: " + 
                              headSequence + ", Tail: " + tailSequence);
        }
        recordTestResult("BasicOps-Sequences", sequencesCorrect, 
                         "HeadSequence and tailSequence test. Head: " + headSequence + 
                         ", Tail: " + tailSequence);
    }

    public void testReadManyItems() throws Exception {
        System.out.println("\n=== Testing Reading Multiple Items ===");
        
        // Add several items
        System.out.println("Adding multiple items...");
        List<String> addedItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String item = "batch-item" + i;
            ringbuffer.add(item);
            addedItems.add(item);
        }
        
        // Test readMany
        System.out.println("Testing readMany operation...");
        CompletionStage<ReadResultSet<String>> resultSetStage = ringbuffer.readManyAsync(0, 5, 10, null);
        
        ReadResultSet<String> resultSet = resultSetStage.toCompletableFuture().get();

        boolean readManyWorked = false;
        if (resultSet.size() == 5) {
            boolean allMatch = true;
            for (int i = 0; i < resultSet.size(); i++) {
                if (!addedItems.get(i).equals(resultSet.get(i))) {
                    allMatch = false;
                    break;
                }
            }
            
            readManyWorked = allMatch;
            if (allMatch) {
                System.out.println("✓ ReadMany operation works correctly");
            } else {
                System.out.println("✗ ReadMany returned incorrect items");
            }
        } else {
            System.out.println("✗ ReadMany returned wrong number of items. Expected: 5, Actual: " + resultSet.size());
        }
        recordTestResult("ReadMany-Basic", readManyWorked, 
                         "ReadMany operation test. Expected size: 5, Actual: " + resultSet.size() + 
                         ", Items matched: " + readManyWorked);
        
        // Test reading from middle
        System.out.println("Testing readMany from middle...");
        CompletionStage<ReadResultSet<String>> middleSetStage = ringbuffer.readManyAsync(3, 3, 10, null);
        
        ReadResultSet<String> middleSet = middleSetStage.toCompletableFuture().get();

        boolean readManyMiddleWorked = middleSet.size() == 3 && 
            "batch-item3".equals(middleSet.get(0)) && 
            "batch-item4".equals(middleSet.get(1)) && 
            "batch-item5".equals(middleSet.get(2));
            
        if (readManyMiddleWorked) {
            System.out.println("✓ ReadMany from middle works correctly");
        } else {
            System.out.println("✗ ReadMany from middle failed");
        }
        recordTestResult("ReadMany-FromMiddle", readManyMiddleWorked, 
                         "ReadMany from middle test. Size: " + middleSet.size() + 
                         ", First item: " + (middleSet.size() > 0 ? middleSet.get(0) : "none") + 
                         ", Last item: " + (middleSet.size() > 2 ? middleSet.get(2) : "none"));
    }

    public void testBlockingOperations() throws Exception {
        System.out.println("\n=== Testing Blocking Operations ===");
        
        // Start a reader thread that will block until data is available
        System.out.println("Starting reader thread with blocking read...");
        final CountDownLatch readLatch = new CountDownLatch(1);
        final boolean[] readSuccess = new boolean[1];
        final String[] readValue = new String[1];
        
        Thread readerThread = new Thread(() -> {
            try {
                // Try to read the next item that doesn't exist yet (will block)
                long currentTail = ringbuffer.tailSequence() + 1;
                readValue[0] = ringbuffer.readOne(currentTail);
                readSuccess[0] = true;
                readLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        readerThread.start();
        
        // Short delay to ensure reader is blocked
        Thread.sleep(1000);
        
        // Add an item to unblock the reader
        System.out.println("Adding item to unblock reader...");
        ringbuffer.add("unblock-item");
        
        // Wait for reader to complete
        boolean completed = readLatch.await(5, TimeUnit.SECONDS);
        
        boolean blockingReadWorked = completed && readSuccess[0] && "unblock-item".equals(readValue[0]);
        if (blockingReadWorked) {
            System.out.println("✓ Blocking read operation works correctly");
        } else {
            System.out.println("✗ Blocking read operation failed or timed out");
        }
        recordTestResult("BlockingOps-Read", blockingReadWorked, 
                         "Blocking read operation test. Completed: " + completed + 
                         ", Success: " + readSuccess[0] + 
                         ", Value: " + readValue[0]);
    }

    public void testAsyncOperations() throws Exception {
        System.out.println("\n=== Testing Async Operations ===");
        
        // Test async add
        System.out.println("Testing async add operation...");
        final CountDownLatch addLatch = new CountDownLatch(1);
        final long[] resultSequence = new long[1];
        
        ringbuffer.addAsync("async-item", OverflowPolicy.OVERWRITE)
            .whenComplete((sequence, throwable) -> {
                if (throwable == null) {
                    resultSequence[0] = sequence;
                    addLatch.countDown();
                }
            });
        
        boolean addCompleted = addLatch.await(5, TimeUnit.SECONDS);
        
        boolean asyncAddWorked = addCompleted && resultSequence[0] == 0;
        if (asyncAddWorked) {
            System.out.println("✓ Async add operation works correctly");
        } else {
            System.out.println("✗ Async add operation failed or timed out");
        }
        recordTestResult("AsyncOps-Add", asyncAddWorked, 
                         "Async add operation test. Completed: " + addCompleted + 
                         ", Sequence: " + resultSequence[0]);
        
        // Test async readOne
        System.out.println("Testing async readOne operation...");
        final CountDownLatch readLatch = new CountDownLatch(1);
        final String[] resultItem = new String[1];
        
        ringbuffer.readManyAsync(0, 1, 1, null)
            .whenComplete((item, throwable) -> {
                if (throwable == null) {
                    resultItem[0] = item.get(0);
                    readLatch.countDown();
                }
            });
        
        boolean readCompleted = readLatch.await(5, TimeUnit.SECONDS);
        
        boolean asyncReadWorked = readCompleted && "async-item".equals(resultItem[0]);
        if (asyncReadWorked) {
            System.out.println("✓ Async readOne operation works correctly");
        } else {
            System.out.println("✗ Async readOne operation failed or timed out");
        }
        recordTestResult("AsyncOps-Read", asyncReadWorked, 
                         "Async readOne operation test. Completed: " + readCompleted + 
                         ", Value: " + resultItem[0]);
    }

    public void testConcurrentAccess() throws Exception {
        System.out.println("\n=== Testing Concurrent Ringbuffer Access ===");
        
        // Number of producer threads
        int numProducers = 3;
        // Items per producer
        int itemsPerProducer = 100;
        // Total items
        int totalItems = numProducers * itemsPerProducer;
        
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch producersCompleteLatch = new CountDownLatch(numProducers);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        System.out.println("Starting " + numProducers + " producer threads...");
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all producers to be ready
                    
                    for (int j = 0; j < itemsPerProducer; j++) {
                        String item = "p" + producerId + "-item" + j;
                        ringbuffer.add(item);
                        successCount.incrementAndGet();
                    }
                    
                    producersCompleteLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        // Start all producers at once
        startLatch.countDown();
        
        // Wait for producers to complete
        boolean completed = producersCompleteLatch.await(30, TimeUnit.SECONDS);
        
        // Verify results
        boolean allItemsAdded = completed && successCount.get() == totalItems;
        if (allItemsAdded) {
            System.out.println("✓ All " + totalItems + " items added successfully");
        } else {
            System.out.println("✗ Failed to add all items. Expected: " + totalItems + 
                              ", Actual: " + successCount.get());
        }
        recordTestResult("ConcurrentAccess-ItemsAdded", allItemsAdded, 
                         "Concurrent item addition test. Expected: " + totalItems + 
                         ", Actual: " + successCount.get());
        
        // Verify ringbuffer size
        long finalSize = ringbuffer.size();
        long expectedSize = Math.min(totalItems, ringbuffer.capacity());
        
        boolean finalSizeCorrect = finalSize == expectedSize;
        if (finalSizeCorrect) {
            System.out.println("✓ Final ringbuffer size is correct: " + finalSize);
        } else {
            System.out.println("✗ Unexpected final size. Expected: " + expectedSize + 
                              ", Actual: " + finalSize);
        }
        recordTestResult("ConcurrentAccess-FinalSize", finalSizeCorrect, 
                         "Final ringbuffer size test. Expected: " + expectedSize + 
                         ", Actual: " + finalSize);
    }
}
