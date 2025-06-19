package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test program for Hazelcast ITopic functionality
 */
public class TopicTest extends AbstractTest {

    private ITopic<String> topic;
    private ITopic<CustomMessage> customTopic;

    public TopicTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        topic = hazelcastInstance.getTopic("test-topic");
        customTopic = hazelcastInstance.getTopic("test-custom-topic");
    }

    @Override
    public void reset() {
        cleanup();
        setup();
    }

    @Override
    public void cleanup() {
        topic.destroy();
        customTopic.destroy();
        topic = null;
        customTopic = null;
    }

    public void testSimplePublishSubscribe() throws Exception {
        System.out.println("\n=== Testing Simple Publish-Subscribe ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] messageReceived = new boolean[1];
        final String testMessage = "Hello, Hazelcast Topic!";
        
        // Add a message listener
        System.out.println("Adding message listener...");
        UUID listenerId = topic.addMessageListener(new MessageListener<String>() {
            @Override
            public void onMessage(Message<String> message) {
                if (testMessage.equals(message.getMessageObject())) {
                    messageReceived[0] = true;
                    latch.countDown();
                }
            }
        });
        
        // Short delay to ensure the listener is registered
        Thread.sleep(500);
        
        // Publish a message
        System.out.println("Publishing message: " + testMessage);
        topic.publish(testMessage);
        
        // Wait for the message to be received
        boolean received = latch.await(5, TimeUnit.SECONDS);
        
        // Remove the listener
        topic.removeMessageListener(listenerId);
        
        if (received && messageReceived[0]) {
            System.out.println("✓ Simple publish-subscribe works correctly");
        } else {
            System.out.println("✗ Simple publish-subscribe failed or timed out");
        }
    }

    public void testMultipleSubscribers() throws Exception {
        System.out.println("\n=== Testing Multiple Subscribers ===");
        
        // Create countdown latches for multiple subscribers
        final int subscriberCount = 3;
        final CountDownLatch latch = new CountDownLatch(subscriberCount);
        final AtomicInteger receivedCount = new AtomicInteger(0);
        final String testMessage = "Broadcast Message";
        
        // Add multiple message listeners
        System.out.println("Adding " + subscriberCount + " message listeners...");
        UUID[] listenerIds = new UUID[subscriberCount];
        
        for (int i = 0; i < subscriberCount; i++) {
            final int subscriberId = i;
            listenerIds[i] = topic.addMessageListener(new MessageListener<String>() {
                @Override
                public void onMessage(Message<String> message) {
                    if (testMessage.equals(message.getMessageObject())) {
                        System.out.println("Subscriber " + subscriberId + " received the message");
                        receivedCount.incrementAndGet();
                        latch.countDown();
                    }
                }
            });
        }
        
        // Short delay to ensure the listeners are registered
        Thread.sleep(500);
        
        // Publish a message
        System.out.println("Publishing message to all subscribers: " + testMessage);
        topic.publish(testMessage);
        
        // Wait for all subscribers to receive the message
        boolean allReceived = latch.await(5, TimeUnit.SECONDS);
        
        // Remove all listeners
        for (UUID listenerId : listenerIds) {
            topic.removeMessageListener(listenerId);
        }
        
        if (allReceived && receivedCount.get() == subscriberCount) {
            System.out.println("✓ Multiple subscribers test passed - all received the message");
        } else {
            System.out.println("✗ Multiple subscribers test failed - expected " + subscriberCount + 
                              " received messages, got " + receivedCount.get());
        }
    }

    public void testMessageListenerRemoval() throws Exception {
        System.out.println("\n=== Testing Message Listener Removal ===");
        
        // Create a flag to track if message was received
        final boolean[] messageReceived = new boolean[1];
        
        // Add a message listener
        System.out.println("Adding message listener...");
        UUID listenerId = topic.addMessageListener(message -> {
            messageReceived[0] = true;
        });
        
        // Short delay to ensure the listener is registered
        Thread.sleep(500);
        
        // Remove the listener
        System.out.println("Removing message listener...");
        boolean removed = topic.removeMessageListener(listenerId);
        
        if (!removed) {
            System.out.println("✗ Failed to remove message listener");
            return;
        }
        
        // Publish a message
        System.out.println("Publishing message after listener removal...");
        topic.publish("This should not be received");
        
        // Short delay to ensure message processing
        Thread.sleep(1000);
        
        if (!messageReceived[0]) {
            System.out.println("✓ Message listener removal works correctly");
        } else {
            System.out.println("✗ Message listener removal failed - message was still received");
        }
    }

    public static class CustomMessage {
        private final String content;
        private final long timestamp;
        
        public CustomMessage(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "CustomMessage{content='" + content + "', timestamp=" + timestamp + "}";
        }
    }

    public void testCustomMessageTypes() throws Exception {
        System.out.println("\n=== Testing Custom Message Types ===");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] correctMessage = new boolean[1];
        
        // Create a custom message
        final CustomMessage testMessage = new CustomMessage("Custom message content");
        
        // Add a message listener for custom messages
        System.out.println("Adding custom message listener...");
        UUID listenerId = customTopic.addMessageListener(new MessageListener<CustomMessage>() {
            @Override
            public void onMessage(Message<CustomMessage> message) {
                CustomMessage received = message.getMessageObject();
                if (received != null && 
                    "Custom message content".equals(received.getContent())) {
                    correctMessage[0] = true;
                    latch.countDown();
                }
            }
        });
        
        // Short delay to ensure the listener is registered
        Thread.sleep(500);
        
        // Publish a custom message
        System.out.println("Publishing custom message...");
        customTopic.publish(testMessage);
        
        // Wait for the message to be received
        boolean received = latch.await(5, TimeUnit.SECONDS);
        
        // Remove the listener
        customTopic.removeMessageListener(listenerId);
        
        if (received && correctMessage[0]) {
            System.out.println("✓ Custom message types work correctly");
        } else {
            System.out.println("✗ Custom message types test failed or timed out");
        }
    }

    public void testReliableTopic() throws Exception {
        System.out.println("\n=== Testing Reliable Topic ===");
        
        // Note: Reliable topics use a different configuration
        // For testing we'll use a regular topic but note the differences
        System.out.println("Note: Reliable topics require specific configuration in hazelcast.xml");
        System.out.println("This test will simulate reliable topic behavior with regular topics");
        
        // Create a countdown latch to synchronize the test
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] messageReceived = new boolean[1];
        
        // Add a message listener
        System.out.println("Adding message listener...");
        UUID listenerId = topic.addMessageListener(message -> {
            if ("Reliable message".equals(message.getMessageObject())) {
                messageReceived[0] = true;
                latch.countDown();
            }
        });
        
        // Short delay to ensure the listener is registered
        Thread.sleep(500);
        
        // Publish a message
        System.out.println("Publishing message...");
        topic.publish("Reliable message");
        
        // Wait for the message to be received
        boolean received = latch.await(5, TimeUnit.SECONDS);
        
        // Remove the listener
        topic.removeMessageListener(listenerId);
        
        if (received && messageReceived[0]) {
            System.out.println("✓ Message delivery works correctly");
            System.out.println("Note: Reliable topic would guarantee delivery even after cluster restarts");
        } else {
            System.out.println("✗ Message delivery failed or timed out");
        }
    }
}
