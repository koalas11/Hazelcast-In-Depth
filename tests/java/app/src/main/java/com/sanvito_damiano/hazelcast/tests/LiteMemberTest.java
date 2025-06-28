package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class LiteMemberTest extends AbstractTest {

    private HazelcastInstance memberInstance1;
    private HazelcastInstance memberInstance2;
    private HazelcastInstance memberInstance3;

    private IMap<String, String> distributedMap;

    public LiteMemberTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {       
        // Configure first member
        Config config1 = new Config();
        config1.setInstanceName("member1");
        config1.setProperty("hazelcast.logging.type", "log4j2");
        
        // Configure second member
        Config config2 = new Config();
        config2.setInstanceName("member2");
        config2.setProperty("hazelcast.logging.type", "log4j2");

        // Configure third member as a lite member
        Config config3 = new Config();
        config3.setInstanceName("member3-lite");
        config3.setProperty("hazelcast.logging.type", "log4j2");
        config3.setLiteMember(true);

        // Configure client
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setInstanceName("client");
        clientConfig.setProperty("hazelcast.logging.type", "log4j2");

        // Start member instances
        memberInstance1 = Hazelcast.newHazelcastInstance(config1);
        memberInstance2 = Hazelcast.newHazelcastInstance(config2);
        memberInstance3 = Hazelcast.newHazelcastInstance(config3);
        hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

        distributedMap = hazelcastInstance.getMap("map");
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

    public void testLiteMemberHasNoData() {
        for (int i = 0; i < 100; i++) {
            distributedMap.put("key" + i, "value" + i);
        }
        
        int size1 = memberInstance1.getMap("map").localKeySet().size();
        int size2 = memberInstance2.getMap("map").localKeySet().size();
        int size3 = memberInstance3.getMap("map").localKeySet().size();

        boolean isLiteMemberEmpty = size3 == 0;
        boolean correctSize = size1 + size2 == 100;

        if (!isLiteMemberEmpty || !correctSize) {
            System.out.println("Test failed: Lite member should have no data, but it has " + size3 + " entries.");
        }
        recordTestResult("LiteMemberTest", isLiteMemberEmpty && correctSize, "Lite member data size expected to be 0, but found " + size3 + ". Sizes of other members: member1 = " + size1 + ", member2 = " + size2);
    }
}
