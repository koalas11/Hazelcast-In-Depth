package com.sanvito_damiano.hazelcast;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;

class MainTest extends JetTestSupport {

    @AfterEach
    public void after() {
        Hazelcast.shutdownAll();
    }

    @Test
    public void testBase() {
        HazelcastInstance instance1 = createHazelcastInstance();
        HazelcastInstance instance2 = createHazelcastInstance();

        assertClusterSize(2, instance1, instance2);

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertTrue(instance1.getLifecycleService().isRunning());
        assertTrue(instance2.getLifecycleService().isRunning());
        assertEquals(2, instance1.getCluster().getMembers().size());
        assertEquals(2, instance2.getCluster().getMembers().size());
    }

    @Test
    public void testPipelineSource() {
        HazelcastInstance instance1 = createHazelcastInstance();
        HazelcastInstance instance2 = createHazelcastInstance();

        assertClusterSize(2, instance1, instance2);

        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items(1, 2, 3, 4))
        .writeTo(Sinks.logger());

        instance1.getJet().newJob(p).join();
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testPipelineStreamingSource() {
        HazelcastInstance instance1 = createHazelcastInstance();
        HazelcastInstance instance2 = createHazelcastInstance();

        assertClusterSize(2, instance1, instance2);

        int itemsPerSecond = 10;
        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.itemStream(itemsPerSecond))
        .withNativeTimestamps(0)
        .writeTo(Sinks.logger());

        Job job = instance1.getJet().newJob(p);

        sleepSeconds(5);
        job.cancel();
    }
}
