package com.sanvito_damiano.hazelcast;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reporting and storing test results
 */
public class TestReporter {
    private String testCategory;
    private List<TestResult> testResults = new ArrayList<>();
    private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public TestReporter(String testCategory) {
        this.testCategory = testCategory;
    }

    /**
     * Records a test result
     */
    public void recordResult(String testName, boolean success, String message) {
        TestResult result = new TestResult(testName, success, message);
        testResults.add(result);
    }
    
    /**
     * Writes all test results to a CSV file
     */
    public String writeResultsToCSV(String folder, String baseFileName) throws IOException {
        String fileName = baseFileName + ".csv";

        // Ensure the directory exists
        Path path = Paths.get("reports", folder);
        path.toFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(Paths.get("reports", folder, fileName).toFile())) {
            // Write CSV header
            writer.append("Test Category,Test Name,Result,Timestamp,Message\n");
            
            // Write each test result
            for (TestResult result : testResults) {
                writer.append(escapeCsvField(testCategory)).append(",")
                      .append(escapeCsvField(result.getTestName())).append(",")
                      .append(result.isSuccess() ? "PASS" : "FAIL").append(",")
                      .append(result.getTimestamp().format(timestampFormatter)).append(",")
                      .append(escapeCsvField(result.getMessage())).append("\n");
            }
            
            System.out.println("\nTest results written to: " + fileName);
            return fileName;
        }
    }
    
    /**
     * Escapes special characters in CSV fields
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    /**
     * Returns summary of test results
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("\n=== Test Summary for %s ===\n", testCategory));
        
        for (TestResult result : testResults) {
            summary.append(result.toString()).append("\n");
        }

        summary.append(String.format("Total Tests: %d, Passed: %d, Failed: %d\n",
                testResults.size(),
                testResults.stream().filter(TestResult::isSuccess).count(),
                testResults.stream().filter(result -> !result.isSuccess()).count()));

        summary.append("========================================\n");
        
        return summary.toString();
    }
    
    /**
     * Get all recorded test results
     */
    public List<TestResult> getTestResults() {
        return new ArrayList<>(testResults);
    }
    
    /**
     * Clear all recorded test results
     */
    public void clearResults() {
        testResults.clear();
    }
}
