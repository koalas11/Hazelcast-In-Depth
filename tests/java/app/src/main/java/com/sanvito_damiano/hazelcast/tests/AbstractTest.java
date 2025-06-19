package com.sanvito_damiano.hazelcast.tests;

import com.hazelcast.core.HazelcastInstance;
import com.sanvito_damiano.hazelcast.TestReporter;

/**
 * Abstract base class for Hazelcast tests
 */
public abstract class AbstractTest {
    protected HazelcastInstance hazelcastInstance;
    protected TestReporter reporter;
    protected String testCategory;
    
    public AbstractTest(HazelcastInstance hazelcastInstance, String testCategory) {
        this.hazelcastInstance = hazelcastInstance;
        this.reporter = new TestReporter(testCategory);
    }
    
    /**
     * Initialize the test with the given Hazelcast instance and category
     */
    public abstract void setup();

    public abstract void reset();

    public abstract void cleanup();
    
    /**
     * Records a test result
     */
    public void recordTestResult(String testName, boolean success, String message) {
        reporter.recordResult(testName, success, message);
    }
    
    /**
     * Generate a report for this test class
     */
    public String generateReport(String folder, String baseFileName) throws Exception {
        return reporter.writeResultsToCSV(folder, baseFileName);
    }
    
    /**
     * Get test summary
     */
    public String getSummary() {
        return reporter.getSummary();
    }
    
    /**
     * Get the test reporter
     */
    public TestReporter getReporter() {
        return reporter;
    }
}
