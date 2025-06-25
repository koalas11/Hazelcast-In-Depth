package com.sanvito_damiano.hazelcast.tests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlService;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionalMap;
import com.hazelcast.sql.SqlRow;

public class FailoverTest extends AbstractTest {

    private IMap<String, Person> personMap;
    private IMap<String, Department> departmentMap;
    private SqlService sqlService;

    public FailoverTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
    }

    @Override
    public void setup() {
        personMap = hazelcastInstance.getMap("person-map");
        departmentMap = hazelcastInstance.getMap("department-map");
        sqlService = hazelcastInstance.getSql();

        sqlService.execute("""
            CREATE OR REPLACE EXTERNAL MAPPING "hazelcast"."public"."persons" EXTERNAL NAME "persons"
            TYPE "IMap"
            OPTIONS (
            'keyFormat'='java',
            'keyJavaClass'='java.lang.String',
            'valueFormat'='java',
            'valueJavaClass'='com.sanvito_damiano.hazelcast.tests.FailoverTest$Person'
            )
        """);

        sqlService.execute("""
            CREATE OR REPLACE EXTERNAL MAPPING "hazelcast"."public"."departments" EXTERNAL NAME "departments"
            TYPE "IMap"
            OPTIONS (
            'keyFormat'='java',
            'keyJavaClass'='java.lang.String',
            'valueFormat'='java',
            'valueJavaClass'='com.sanvito_damiano.hazelcast.tests.FailoverTest$Department'
            )
        """);
    }

    @Override
    public void reset() {
        personMap.clear();
        departmentMap.clear();
        
        // Add department data
        departmentMap.put("D1", new Department("D1", "Engineering", "Building A"));
        departmentMap.put("D2", new Department("D2", "Marketing", "Building A"));
        departmentMap.put("D3", new Department("D3", "Finance", "Building B"));
        
        // Add person data
        personMap.put("p1", new Person("Alice", 30, true, "D1"));
        personMap.put("p2", new Person("Bob", 25, true, "D1"));
        personMap.put("p3", new Person("Charlie", 35, false, "D2"));
        personMap.put("p4", new Person("Diana", 28, true, "D2"));
        personMap.put("p5", new Person("Edward", 22, false, "D3"));
        personMap.put("p6", new Person("Frank", 40, true, "D1"));
        personMap.put("p7", new Person("Grace", 29, false, "D2"));
        personMap.put("p8", new Person("Helen", 45, true, "D4"));
        // Add more data to ensure sufficient load
        for (int i = 9; i <= 100; i++) {
            personMap.put("p" + i, new Person("Person" + i, 20 + (i % 30), i % 2 == 0, "D" + (i % 4 + 1)));
        }
    }

    @Override
    public void cleanup() {
        personMap.destroy();
        personMap = null;
        departmentMap.destroy();
        departmentMap = null;
        sqlService = null;
    }

    private HazelcastInstance createNewHazelcastInstance() {
        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setInstanceName("special_member");
        config.setProperty("hazelcast.logging.type", "log4j2");
        return Hazelcast.newHazelcastInstance(config);
    }
    
    /**
     * Tests SQL query resilience when a node is shutdown during query execution
     * @throws InterruptedException 
     */
    public void testSqlQueryDuringNodeShutdown() throws InterruptedException {
        System.out.println("\n=== Testing SQL Query During Node Shutdown ===");

        HazelcastInstance nodeToShutdown = createNewHazelcastInstance();

        Thread.sleep(5000); // Wait for the new node to join the cluster
        
        final AtomicBoolean runThread = new AtomicBoolean(true);
        final CountDownLatch queryStarted = new CountDownLatch(1);
        final AtomicBoolean querySucceeded = new AtomicBoolean(false);
        
        // Start long-running query in a separate thread
        Thread queryThread = new Thread(() -> {
            try {
                System.out.println("Starting complex SQL query...");
                queryStarted.countDown();
                
                do {
                    // Complex query involving joins and aggregations to ensure it takes some time
                    try (SqlResult result = sqlService.execute("""
                        SELECT p.name, p.age, d.name as department_name
                        FROM persons p
                        JOIN departments d ON p.departmentId = d.id
                        WHERE p.age > 20 AND d.location = 'Building A'
                        ORDER BY p.age DESC
                    """)) {
                        
                        
                        // Process results
                        List<SqlRow> rows = new ArrayList<>();
                        result.iterator().forEachRemaining(rows::add);
                        
                        System.out.println("Query completed successfully with " + rows.size() + " rows");
                        
                        querySucceeded.set(true);
                    }
                } while (runThread.get());
            } catch (Exception e) {
                System.out.println("Query failed: " + e.getMessage());
            }
        });
        
        // Wait for query to start
        try {
            queryThread.start();
            boolean started = queryStarted.await(10, TimeUnit.SECONDS);
            if (!started) {
                System.out.println("⚠ Query did not start within timeout");
                recordTestResult("Failover-SqlQueryShutdown", false, "Query did not start within timeout");
                return;
            }
        
            nodeToShutdown.shutdown();

            runThread.set(false); // Stop the query thread after shutdown

            Thread.sleep(2000); // Allow some time for the node to shutdown

            queryThread.join(4000); // Wait for the query thread to finish
            
            boolean testResult = querySucceeded.get();
            if (testResult) {
                System.out.println("✓ SQL query completed successfully despite node shutdown");
            } else {
                System.out.println("✗ SQL query failed during node shutdown");
            }
            
            recordTestResult("Failover-SqlQueryShutdown", testResult, 
                    testResult ? "SQL query was resilient to node shutdown" : 
                                "SQL query failed during node shutdown");
            
        } catch (Exception e) {
            System.out.println("✗ Test failed with exception: " + e.getMessage());
            recordTestResult("Failover-SqlQueryShutdown", false, "Test failed with exception: " + e.getMessage());
        }
    }
    
    /**
     * Tests SQL query resilience when a node is terminated during query execution
     * @throws InterruptedException 
     */
    public void testSqlQueryDuringNodeTermination() throws InterruptedException {
        System.out.println("\n=== Testing SQL Query During Node Termination ===");

        HazelcastInstance nodeToTerminate = createNewHazelcastInstance();

        Thread.sleep(5000); // Wait for the new node to join the cluster
        
        final AtomicBoolean runThread = new AtomicBoolean(true);
        final CountDownLatch queryStarted = new CountDownLatch(1);
        final AtomicBoolean querySucceeded = new AtomicBoolean(false);
        
        // Start long-running query in a separate thread
        Thread queryThread = new Thread(() -> {
            try {
                System.out.println("Starting complex SQL query...");
                queryStarted.countDown();
                
                do {
                    // Complex query involving joins and aggregations to ensure it takes some time
                    try (SqlResult result = sqlService.execute("""
                        SELECT p.name, p.age, d.name as department_name
                        FROM persons p
                        JOIN departments d ON p.departmentId = d.id
                        WHERE p.age > 20 AND d.location = 'Building A'
                        ORDER BY p.age DESC
                    """)) {
                        
                        
                        // Process results
                        List<SqlRow> rows = new ArrayList<>();
                        result.iterator().forEachRemaining(rows::add);
                        
                        System.out.println("Query completed successfully with " + rows.size() + " rows");
                        
                        querySucceeded.set(true);
                    }
                } while (runThread.get());
            } catch (Exception e) {
                System.out.println("Query failed: " + e.getMessage());
            }
        });
        
        // Wait for query to start
        try {
            queryThread.start();
            boolean started = queryStarted.await(10, TimeUnit.SECONDS);
            if (!started) {
                System.out.println("⚠ Query did not start within timeout");
                recordTestResult("Failover-SqlQueryTermination", false, "Query did not start within timeout");
                return;
            }
        
            nodeToTerminate.getLifecycleService().terminate();

            runThread.set(false); // Stop the query thread after termination

            Thread.sleep(2000); // Allow some time for the node to terminate

            queryThread.join(4000); // Wait for the query thread to finish
            
            boolean testResult = querySucceeded.get();
            if (testResult) {
                System.out.println("✓ SQL query completed successfully despite node termination");
            } else {
                System.out.println("✗ SQL query failed during node termination");
            }
            
            recordTestResult("Failover-SqlQueryTermination", testResult, 
                    testResult ? "SQL query was resilient to node termination" : 
                                "SQL query failed during node termination");
            
        } catch (Exception e) {
            System.out.println("✗ Test failed with exception: " + e.getMessage());
            recordTestResult("Failover-SqlQueryTermination", false, "Test failed with exception: " + e.getMessage());
        }
    }
    
    /**
        * Tests Predicate API query resilience when a node is shutdown during query execution
     * @throws InterruptedException 
        */
    public void testPredicateQueryDuringNodeShutdown() throws InterruptedException {
        System.out.println("\n=== Testing Predicate API Query During Node Shutdown ===");

        HazelcastInstance nodeToShutdown = createNewHazelcastInstance();

        Thread.sleep(5000); // Wait for the new node to join the cluster
        
        final AtomicBoolean runThread = new AtomicBoolean(true);
        final CountDownLatch queryStarted = new CountDownLatch(1);
        final AtomicBoolean querySucceeded = new AtomicBoolean(false);
        
        // Start long-running query in a separate thread
        Thread queryThread = new Thread(() -> {
            try {
                System.out.println("Starting complex Predicate query...");
                queryStarted.countDown();
                
                do {
                    // Create a complex predicate to ensure the query takes some time
                    Predicate<String, Person> complexPredicate = Predicates.or(
                        Predicates.and(
                            Predicates.greaterThan("age", 25),
                            Predicates.equal("active", true)
                        ),
                        Predicates.and(
                            Predicates.lessThan("age", 20),
                            Predicates.equal("departmentId", "D3")
                        )
                    );
                    
                    // Execute the query
                    Collection<Person> results = personMap.values(complexPredicate);

                    System.out.println("Predicate query completed successfully with " + results.size() + " results");
                    querySucceeded.set(true);
                } while (runThread.get());
            } catch (Exception e) {
                System.out.println("Query failed: " + e.getMessage());
            }
        });
        
        // Wait for query to start
        try {
            queryThread.start();
            boolean started = queryStarted.await(10, TimeUnit.SECONDS);
            if (!started) {
                System.out.println("⚠ Query did not start within timeout");
                recordTestResult("Failover-PredicateQueryShutdown", false, "Query did not start within timeout");
                return;
            }
        
            nodeToShutdown.shutdown();

            runThread.set(false); // Stop the query thread after shutdown

            Thread.sleep(2000); // Allow some time for the node to shutdown

            queryThread.join(4000); // Wait for the query thread to finish
            
            boolean testResult = querySucceeded.get();
            if (testResult) {
                System.out.println("✓ Predicate query completed successfully despite node shutdown");
            } else {
                System.out.println("✗ Predicate query failed during node shutdown");
            }
            
            recordTestResult("Failover-PredicateQueryShutdown", testResult, 
                    testResult ? "Predicate query was resilient to node shutdown" : 
                                "Predicate query failed during node shutdown");
            
        } catch (Exception e) {
            System.out.println("✗ Test failed with exception: " + e.getMessage());
            recordTestResult("Failover-PredicateQueryShutdown", false, "Test failed with exception: " + e.getMessage());
        }
    }
    
    /**
        * Tests Predicate API query resilience when a node is terminated during query execution
     * @throws InterruptedException 
        */
    public void testPredicateQueryDuringNodeTermination() throws InterruptedException {
        System.out.println("\n=== Testing Predicate API Query During Node Termination ===");

        HazelcastInstance nodeToTerminate = createNewHazelcastInstance();

        Thread.sleep(5000); // Wait for the new node to join the cluster
        
        final AtomicBoolean runThread = new AtomicBoolean(true);
        final CountDownLatch queryStarted = new CountDownLatch(1);
        final AtomicBoolean querySucceeded = new AtomicBoolean(false);
        
        // Start long-running query in a separate thread
        Thread queryThread = new Thread(() -> {
            try {
                System.out.println("Starting complex Predicate query...");
                queryStarted.countDown();
                
                do {
                    // Create a complex predicate to ensure the query takes some time
                    Predicate<String, Person> complexPredicate = Predicates.or(
                        Predicates.and(
                            Predicates.greaterThan("age", 25),
                            Predicates.equal("active", true)
                        ),
                        Predicates.and(
                            Predicates.lessThan("age", 20),
                            Predicates.equal("departmentId", "D3")
                        )
                    );
                    
                    // Execute the query
                    Collection<Person> results = personMap.values(complexPredicate);

                    System.out.println("Predicate query completed successfully with " + results.size() + " results");
                    querySucceeded.set(true);
                } while (runThread.get());
            } catch (Exception e) {
                System.out.println("Query failed: " + e.getMessage());
            }
        });
        
        // Wait for query to start
        try {
            queryThread.start();
            boolean started = queryStarted.await(10, TimeUnit.SECONDS);
            if (!started) {
                System.out.println("⚠ Query did not start within timeout");
                recordTestResult("Failover-PredicateQueryTermination", false, "Query did not start within timeout");
                return;
            }
        
            nodeToTerminate.getLifecycleService().terminate();

            runThread.set(false); // Stop the query thread after termination

            Thread.sleep(2000); // Allow some time for the node to terminate

            queryThread.join(4000); // Wait for the query thread to finish
            
            boolean testResult = querySucceeded.get();
            if (testResult) {
                System.out.println("✓ Predicate query completed successfully despite node termination");
            } else {
                System.out.println("✗ Predicate query failed during node termination");
            }
            
            recordTestResult("Failover-PredicateQueryTermination", testResult, 
                    testResult ? "Predicate query was resilient to node termination" : 
                                "Predicate query failed during node termination");
            
        } catch (Exception e) {
            System.out.println("✗ Test failed with exception: " + e.getMessage());
            recordTestResult("Failover-PredicateQueryTermination", false, "Test failed with exception: " + e.getMessage());
        }
    }
    
    /**
        * Tests transaction resilience when a node is shutdown during transaction execution
     * @throws InterruptedException 
        */
    public void testTransactionDuringNodeShutdown() throws InterruptedException {
        System.out.println("\n=== Testing Transaction During Node Shutdown ===");
        
        HazelcastInstance nodeToShutdown = createNewHazelcastInstance();

        Thread.sleep(5000); // Wait for the new node to join the cluster
        
        final CountDownLatch txStarted = new CountDownLatch(1);
        
        // Start transaction in a separate thread
        Thread txThread = new Thread(() -> {
            try {
                System.out.println("Starting transaction...");
                
                // Configure transaction with longer timeout
                TransactionOptions txOptions = TransactionOptions.getDefault().setTimeout(60, TimeUnit.SECONDS);
                
                // Begin transaction
                TransactionContext txContext = hazelcastInstance.newTransactionContext(txOptions);
                txContext.beginTransaction();
                
                // Signal that transaction has started
                txStarted.countDown();
                
                // Get transactional map
                TransactionalMap<String, Person> txPersonMap = txContext.getMap("persons");
                
                // Perform multiple operations in transaction
                txPersonMap.put("p6", new Person("Frank", 35, true, "D1"));
                
                // Sleep to simulate long transaction
                Thread.sleep(5000);
                
                txPersonMap.put("p7", new Person("Grace", 29, false, "D2"));
                txPersonMap.delete("p5"); // Remove Edward
                
                // Commit transaction
                txContext.commitTransaction();              
            } catch (Exception e) {
                System.out.println("Transaction failed: " + e.getMessage());
            }
        });
        
        // Wait for transaction to start
        try {
            txThread.start();
            boolean started = txStarted.await(10, TimeUnit.SECONDS);
            if (!started) {
                System.out.println("⚠ Transaction did not start within timeout");
                recordTestResult("Failover-TransactionShutdown", false, "Transaction did not start within timeout");
                return;
            }
            
            nodeToShutdown.shutdown();
            
            txThread.join(4000); // Wait for the transaction thread to finish

            // Verify transaction results
            boolean verifyAdd = personMap.containsKey("p6") && personMap.containsKey("p7");
            boolean verifyDelete = !personMap.containsKey("p5");
                        
            boolean testResult = verifyAdd && verifyDelete;
            System.out.println("Transaction " + (testResult ? "completed successfully" : "failed verification"));

            if (testResult) {
                System.out.println("✓ Transaction completed successfully despite node shutdown");
            } else {
                System.out.println("✗ Transaction failed during node shutdown");
            }
            
            recordTestResult("Failover-TransactionShutdown", testResult, 
                    testResult ? "Transaction was resilient to node shutdown" : 
                                "Transaction failed during node shutdown");
        } catch (Exception e) {
            System.out.println("✗ Test failed with exception: " + e.getMessage());
            recordTestResult("Failover-TransactionShutdown", false, "Test failed with exception: " + e.getMessage());
        }
    }
    
    /**
        * Tests transaction resilience when a node is terminated during transaction execution
     * @throws InterruptedException 
        */
    public void testTransactionDuringNodeTermination() throws InterruptedException {
        System.out.println("\n=== Testing Transaction During Node Termination ===");
        
        HazelcastInstance nodeToTerminate = createNewHazelcastInstance();

        Thread.sleep(5000); // Wait for the new node to join the cluster

        final CountDownLatch txStarted = new CountDownLatch(1);
        
        // Start transaction in a separate thread
        Thread txThread = new Thread(() -> {
            try {
                System.out.println("Starting transaction...");
                
                // Configure transaction with longer timeout
                TransactionOptions txOptions = TransactionOptions.getDefault().setTimeout(60, TimeUnit.SECONDS);
                
                // Begin transaction
                TransactionContext txContext = hazelcastInstance.newTransactionContext(txOptions);
                txContext.beginTransaction();
                
                // Signal that transaction has started
                txStarted.countDown();
                
                // Get transactional maps
                TransactionalMap<String, Person> txPersonMap = txContext.getMap("persons");
                TransactionalMap<String, Department> txDeptMap = txContext.getMap("departments");
                
                // Perform multiple operations in transaction
                txPersonMap.put("p8", new Person("Helen", 45, true, "D4"));
                
                // Sleep to simulate long transaction
                Thread.sleep(4000);
                
                txDeptMap.put("D4", new Department("D4", "Research", "Building C"));
                txPersonMap.delete("p4"); // Remove Diana
                
                // Commit transaction
                txContext.commitTransaction();
            } catch (Exception e) {
                System.out.println("Transaction failed: " + e.getMessage());
            }
        });
        
        // Wait for transaction to start
        try {
            txThread.start();
            boolean started = txStarted.await(5, TimeUnit.SECONDS);
            if (!started) {
                System.out.println("⚠ Transaction did not start within timeout");
                recordTestResult("Failover-TransactionTermination", false, "Transaction did not start within timeout");
                return;
            }
            
            nodeToTerminate.getLifecycleService().terminate();
            
            txThread.join(4000); // Wait for the transaction thread to finish            

            // Verify transaction results
            boolean verifyPersonAdd = personMap.containsKey("p8");
            boolean verifyDeptAdd = departmentMap.containsKey("D4");
            boolean verifyDelete = !personMap.containsKey("p4");

            boolean testResult = verifyPersonAdd && verifyDeptAdd && verifyDelete;

            System.out.println("Transaction " + (testResult ? "completed successfully" : "failed verification"));

            if (testResult) {
                System.out.println("✓ Transaction completed successfully despite node termination");
            } else {
                System.out.println("✗ Transaction failed during node termination");
            }
            
            recordTestResult("Failover-TransactionTermination", testResult, 
                    testResult ? "Transaction was resilient to node termination" : 
                                "Transaction failed during node termination");
            
        } catch (Exception e) {
            System.out.println("✗ Test failed with exception: " + e.getMessage());
            recordTestResult("Failover-TransactionTermination", false, "Test failed with exception: " + e.getMessage());
        }
    }
    
    // Department class for testing
    private static class Department implements Serializable {
        private String id;
        private String name;
        private String location;
        
        public Department(String id, String name, String location) {
            this.id = id;
            this.name = name;
            this.location = location;
        }

        @Override
        public String toString() {
            return "Department{id='" + id + "', name='" + name + "', location='" + location + "'}";
        }
    }

    // Person class for testing
    private static class Person implements Serializable {
        private String name;
        private int age;
        private boolean active;
        private String departmentId;
        
        public Person(String name, int age, boolean active, String departmentId) {
            this.name = name;
            this.age = age;
            this.active = active;
            this.departmentId = departmentId;
        }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", active=" + active + ", departmentId='" + departmentId + "'}";
        }
    }
}
