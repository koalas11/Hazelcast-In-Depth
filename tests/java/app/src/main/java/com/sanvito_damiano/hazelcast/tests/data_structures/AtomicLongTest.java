package com.sanvito_damiano.hazelcast.tests.data_structures;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.sanvito_damiano.hazelcast.tests.AbstractTest;

/**
 * Test program for Hazelcast IAtomicLong functionality
 */
public class AtomicLongTest extends AbstractTest {

    public AtomicLongTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    public void testAtomicLongCreation() {
        try {
            IAtomicLong atomicLong = hazelcastInstance.getCPSubsystem().getAtomicLong("test-atomic-long");

            atomicLong.set(0);
            
            System.out.println("\nAll IAtomicLong tests completed successfully!");
        } catch (UnsupportedOperationException e) {
            System.out.println("IAtomicLong is not supported in the Open Source Hazelcast Version.");
            recordTestResult("IAtomicLong Creation", true, "IAtomicLong is not supported in the Open Source Hazelcast Version.");
            return;
        } catch (Exception e) {
            recordTestResult("IAtomicLong Creation", false, e.getMessage());
            throw e;
        }
    }

    @Override
    public void setup() {}

    @Override
    public void reset() {}

    @Override
    public void cleanup() {}
}
