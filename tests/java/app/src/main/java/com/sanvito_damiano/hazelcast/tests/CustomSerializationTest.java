package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class CustomSerializationTest extends AbstractTest {

    private HazelcastInstance memberInstance1;
    @SuppressWarnings("unused")
    private HazelcastInstance memberInstance2;
    @SuppressWarnings("unused")
    private HazelcastInstance memberInstance3;

    private IMap<String, TestSerializableObj> distributedMap;

    public CustomSerializationTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        
        // Configure first member
        Config config1 = new Config();
        config1.setInstanceName("member1");
        config1.setProperty("hazelcast.logging.type", "log4j2");
        config1.getSerializationConfig().getCompactSerializationConfig().addSerializer(new TestSerializableObjSerializer());
        
        // Configure second member
        Config config2 = new Config();
        config2.setInstanceName("member2");
        config2.setProperty("hazelcast.logging.type", "log4j2");
        config2.getSerializationConfig().getCompactSerializationConfig().addSerializer(new TestSerializableObjSerializer());

        Config config3 = new Config();
        config3.setInstanceName("member3");
        config3.setProperty("hazelcast.logging.type", "log4j2");
        config3.getSerializationConfig().getCompactSerializationConfig().addSerializer(new TestSerializableObjSerializer());

        // Configure client
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setInstanceName("client");
        clientConfig.setProperty("hazelcast.logging.type", "log4j2");
        clientConfig.getSerializationConfig().getCompactSerializationConfig().addClass(TestSerializableObj.class);

        // Start member instances
        memberInstance1 = Hazelcast.newHazelcastInstance(config1);
        memberInstance2 = Hazelcast.newHazelcastInstance(config2);
        memberInstance3 = Hazelcast.newHazelcastInstance(config3);
        hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

        distributedMap = hazelcastInstance.getMap("custom-serialization-map");
    }

    @Override
    public void reset() {
        distributedMap.clear();
    }

    @Override
    public void cleanup() {
        distributedMap.destroy();
        hazelcastInstance.shutdown();
        memberInstance1.getCluster().shutdown();
    }

    public static class TestSerializableObj {
        private String name;
        private int age;

        public TestSerializableObj(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    public static class TestSerializableObjSerializer implements CompactSerializer<TestSerializableObj> {
        @SuppressWarnings("null")
        @Override
        public TestSerializableObj read(CompactReader reader) {
            String name = reader.readString("name");
            int age = reader.readInt32("age");
            return new TestSerializableObj(name, age);
        }

        @Override
        public void write(@SuppressWarnings("null") CompactWriter writer, @SuppressWarnings("null") TestSerializableObj employee) {
            writer.writeString("name", employee.getName());
            writer.writeInt32("age", employee.getAge());
        }

        @SuppressWarnings("null")
        @Override
        public Class<TestSerializableObj> getCompactClass() {
            return TestSerializableObj.class;
        }

        @SuppressWarnings("null")
        @Override
        public String getTypeName() {
            return "testSerializableObj";
        }
    }

    /*
     * Test for custom serialization of a serializable object
     * This test checks if the custom serializer correctly serializes and deserializes
     */
    public void testCustomSerialization() throws Exception {
        System.out.println("\n=== Testing Custom Serialization ===");

        // Create a test object
        TestSerializableObj testObj = new TestSerializableObj("TestName", 30);
        distributedMap.put("testObj", testObj);

        // Retrieve the object
        TestSerializableObj retrievedObj = distributedMap.get("testObj");

        // Verify the object properties
        boolean isRetrivedCorrect = retrievedObj != null && 
                "TestName".equals(retrievedObj.getName()) && 
                retrievedObj.getAge() == 30;

        if (isRetrivedCorrect) {
            System.out.println("✓ Custom serialization works correctly");
        } else {
            System.out.println("✗ Custom serialization failed");
        }
        recordTestResult("CustomSerialization", isRetrivedCorrect, "Custom serialization test completed expectations met: " + isRetrivedCorrect);
    }
}
