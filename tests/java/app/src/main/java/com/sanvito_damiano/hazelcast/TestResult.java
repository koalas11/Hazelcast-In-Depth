package com.sanvito_damiano.hazelcast;

import java.time.LocalDateTime;

/**
 * Represents the result of a single test operation
 */
public class TestResult {
    private String testName;
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    
    public TestResult(String testName, boolean success, String message) {
        this.testName = testName;
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getTestName() { return testName; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("[%s]: %s - %s", 
                             testName, 
                             success ? "PASS" : "FAIL", 
                             message);
    }
}
