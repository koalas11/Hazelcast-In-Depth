package com.sanvito_damiano.hazelcast.tests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.hazelcast.sql.SqlResult;
import com.hazelcast.sql.SqlRow;
import com.hazelcast.sql.SqlService;

public class QueryTest extends AbstractTest {

    private IMap<String, Person2> personMap;
    private IMap<String, Department2> departmentMap;
    private SqlService sqlService;

    public QueryTest(HazelcastInstance hazelcastInstance, String testCategory) {
        super(hazelcastInstance, testCategory);
        this.testCategory = testCategory;
    }

    @Override
    public void setup() {
        personMap = hazelcastInstance.getMap("persons");
        departmentMap = hazelcastInstance.getMap("departments");
        sqlService = hazelcastInstance.getSql();

        sqlService.execute("""
            CREATE OR REPLACE EXTERNAL MAPPING "hazelcast"."public"."persons" EXTERNAL NAME "persons"
            TYPE "IMap"
            OPTIONS (
            'keyFormat'='java',
            'keyJavaClass'='java.lang.String',
            'valueFormat'='java',
            'valueJavaClass'='com.sanvito_damiano.hazelcast.tests.Person2'
            )
        """);

        sqlService.execute("""
            CREATE OR REPLACE EXTERNAL MAPPING "hazelcast"."public"."departments" EXTERNAL NAME "departments"
            TYPE "IMap"
            OPTIONS (
            'keyFormat'='java',
            'keyJavaClass'='java.lang.String',
            'valueFormat'='java',
            'valueJavaClass'='com.sanvito_damiano.hazelcast.tests.Department2'
            )
        """);
    }

    @Override
    public void reset() {
        personMap.clear();
        departmentMap.clear();
        
        // Add person test data
        personMap.put("p1", new Person2("Alice", 32, true, "D1"));
        personMap.put("p2", new Person2("Bob", 24, true, "D1"));
        personMap.put("p3", new Person2("Charlie", 29, true, "D2"));
        personMap.put("p4", new Person2("Diana", 41, false, "D2"));
        personMap.put("p5", new Person2("Edward", 18, false, "D3"));
        
        // Add department test data
        departmentMap.put("D1", new Department2("D1", "Engineering", "Building A"));
        departmentMap.put("D2", new Department2("D2", "Marketing", "Building B"));
        departmentMap.put("D3", new Department2("D3", "HR", "Building A"));
    }

    @Override
    public void cleanup() {
        personMap.destroy();
        personMap = null;
        departmentMap.destroy();
        departmentMap = null;
        sqlService = null;
    }

    public void testSimpleSqlQueries() {
        System.out.println("\n=== Testing Simple SQL Queries ===");
        
        // Test simple SELECT query
        System.out.println("Testing simple SELECT query...");
        
        try (SqlResult result = sqlService.execute("SELECT * FROM persons")) {
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean simpleQueryWorks = rows.size() == 5;
            if (simpleQueryWorks) {
                System.out.println("✓ Simple SELECT query works correctly");
            } else {
                System.out.println("✗ Simple SELECT query failed. Expected 5 rows, got: " + rows.size());
            }
            recordTestResult("SqlQuery-SimpleSelect", simpleQueryWorks, 
                                "Simple SELECT query test. Expected 5 rows, got: " + rows.size());
        }
    }
    
    public void testFilteredSqlQueries() {
        System.out.println("\n=== Testing Filtered SQL Queries ===");
        
        // Test WHERE clause
        System.out.println("Testing WHERE clause...");
        
        try (SqlResult result = sqlService.execute("SELECT * FROM persons WHERE age > 30")) {
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean whereQueryWorks = rows.size() == 2;
            if (whereQueryWorks) {
                System.out.println("✓ WHERE clause query works correctly");
            } else {
                System.out.println("✗ WHERE clause query failed. Expected 2 rows, got: " + rows.size());
            }
            recordTestResult("SqlQuery-Where", whereQueryWorks, 
                                "WHERE clause query test. Expected 2 rows, got: " + rows.size());
        }
        
        // Test ORDER BY clause
        System.out.println("Testing ORDER BY clause...");
        
        try (SqlResult result = sqlService.execute("SELECT * FROM persons ORDER BY age DESC")) {
            SqlRow firstRow = result.iterator().next();
            String name = firstRow.getObject("name");
            
            boolean orderByQueryWorks = "Diana".equals(name);
            if (orderByQueryWorks) {
                System.out.println("✓ ORDER BY clause query works correctly");
            } else {
                System.out.println("✗ ORDER BY clause query failed. Expected 'Diana', got: " + name);
            }
            recordTestResult("SqlQuery-OrderBy", orderByQueryWorks, 
                                "ORDER BY clause query test. Expected 'Diana', got: " + name);
        }
    }
    
    public void testJoinSqlQueries() {
        System.out.println("\n=== Testing JOIN SQL Queries ===");
        
        // Test INNER JOIN
        System.out.println("Testing INNER JOIN...");
        
        try (SqlResult result = sqlService.execute(
                "SELECT p.name, d.name as department_name " +
                "FROM persons p " +
                "JOIN departments d ON p.departmentId = d.id")) {
            
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean joinQueryWorks = rows.size() == 5;
            if (joinQueryWorks) {
                System.out.println("✓ INNER JOIN query works correctly");
            } else {
                System.out.println("✗ INNER JOIN query failed. Expected 5 rows, got: " + rows.size());
            }
            recordTestResult("SqlQuery-InnerJoin", joinQueryWorks, 
                                "INNER JOIN query test. Expected 5 rows, got: " + rows.size());
        }
        
        // Test JOIN with WHERE
        System.out.println("Testing JOIN with WHERE...");
        
        try (SqlResult result = sqlService.execute(
                "SELECT p.name, d.name as department_name " +
                "FROM persons p " +
                "JOIN departments d ON p.departmentId = d.id " +
                "WHERE d.location = 'Building A'")) {
            
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean joinWhereQueryWorks = rows.size() == 3;
            if (joinWhereQueryWorks) {
                System.out.println("✓ JOIN with WHERE query works correctly");
            } else {
                System.out.println("✗ JOIN with WHERE query failed. Expected 3 rows, got: " + rows.size());
            }
            recordTestResult("SqlQuery-JoinWhere", joinWhereQueryWorks, 
                                "JOIN with WHERE query test. Expected 3 rows, got: " + rows.size());
        }
    }
    
    public void testAggregationSqlQueries() {
        System.out.println("\n=== Testing Aggregation SQL Queries ===");
        
        // Test GROUP BY with aggregation
        System.out.println("Testing GROUP BY with aggregation...");
        
        try (SqlResult result = sqlService.execute(
                "SELECT departmentId, COUNT(*) as employee_count, AVG(age) as avg_age " +
                "FROM persons " +
                "GROUP BY departmentId")) {
            
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean groupByQueryWorks = rows.size() == 3;
            if (groupByQueryWorks) {
                System.out.println("✓ GROUP BY with aggregation query works correctly");
            } else {
                System.out.println("✗ GROUP BY with aggregation query failed. Expected 3 rows, got: " + rows.size());
            }
            recordTestResult("SqlQuery-GroupBy", groupByQueryWorks, 
                                "GROUP BY with aggregation query test. Expected 3 rows, got: " + rows.size());
        }
        
        // Test HAVING clause
        System.out.println("Testing HAVING clause...");
        
        try (SqlResult result = sqlService.execute(
                "SELECT departmentId, COUNT(*) as employee_count " +
                "FROM persons " +
                "GROUP BY departmentId " +
                "HAVING COUNT(*) > 1")) {
            
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean havingQueryWorks = rows.size() == 2;
            if (havingQueryWorks) {
                System.out.println("✓ HAVING clause query works correctly");
            } else {
                System.out.println("✗ HAVING clause query failed. Expected 2 rows, got: " + rows.size());
            }
            recordTestResult("SqlQuery-Having", havingQueryWorks, 
                                "HAVING clause query test. Expected 2 rows, got: " + rows.size());
        }
    }
    
    public void testComparisonWithPredicates() {
        System.out.println("\n=== Comparing SQL with Predicate API ===");
        
        // Test simple filtering: people over 30
        System.out.println("Testing simple filtering comparison...");
        
        // SQL approach
        long startTimeSql = System.nanoTime();
        List<Person2> sqlResults = new ArrayList<>();
        
        try (SqlResult result = sqlService.execute("SELECT * FROM persons WHERE age > 30")) {
            result.iterator().forEachRemaining(row -> {
                String name = row.getObject("name");
                int age = row.getObject("age");
                boolean active = row.getObject("active");
                String deptId = row.getObject("departmentId");
                sqlResults.add(new Person2(name, age, active, deptId));
            });
        }
        long sqlTime = System.nanoTime() - startTimeSql;
        
        // Predicate approach
        long startTimePredicate = System.nanoTime();
        Predicate<String, Person2> olderThan30 = Predicates.greaterThan("age", 30);
        Collection<Person2> predicateResults = personMap.values(olderThan30);
        long predicateTime = System.nanoTime() - startTimePredicate;
        
        // Compare results
        boolean resultsSizeMatch = sqlResults.size() == predicateResults.size();
        boolean allNamesMatch = true;
        
        for (Person2 sqlPerson : sqlResults) {
            final String sqlName = sqlPerson.getName();
            boolean found = predicateResults.stream().anyMatch(p -> sqlName.equals(p.getName()));
            if (!found) {
                allNamesMatch = false;
                break;
            }
        }
        
        boolean resultsMatch = resultsSizeMatch && allNamesMatch;
        if (resultsMatch) {
            System.out.println("✓ SQL and Predicate API returned the same results");
        } else {
            System.out.println("✗ SQL and Predicate API returned different results");
        }
        
        System.out.println("  SQL query execution time: " + TimeUnit.NANOSECONDS.toMicros(sqlTime) + " μs");
        System.out.println("  Predicate query execution time: " + TimeUnit.NANOSECONDS.toMicros(predicateTime) + " μs");
        
        recordTestResult("Comparison-SimpleFilter", resultsMatch, 
                            "Comparing SQL vs Predicate API. Results match: " + resultsMatch + 
                            ", SQL time: " + TimeUnit.NANOSECONDS.toMicros(sqlTime) + "μs" +
                            ", Predicate time: " + TimeUnit.NANOSECONDS.toMicros(predicateTime) + "μs");
    }
    
    public void testComplexPredicateQueries() {
        System.out.println("\n=== Testing Complex Predicate Queries ===");
        
        // Test complex AND/OR predicates
        System.out.println("Testing complex predicate (age > 25 AND active = true) OR departmentId = 'D3'...");
        
        Predicate<String, Person2> complexPredicate = Predicates.or(
            Predicates.and(
                Predicates.greaterThan("age", 25),
                Predicates.equal("active", true)
            ),
            Predicates.equal("departmentId", "D3")
        );
        
        Collection<Person2> results = personMap.values(complexPredicate);
        
        boolean predicateSuccess = results.size() == 3; // Alice, Charlie, Edward
        if (predicateSuccess) {
            System.out.println("✓ Complex predicate query works correctly");
        } else {
            System.out.println("✗ Complex predicate query failed. Expected 3 people, got: " + results.size());
        }
        recordTestResult("Predicate-ComplexQuery", predicateSuccess, 
                            "Complex predicate query test. Expected 3 people, Got: " + results.size());
        
        // Test regex predicate
        System.out.println("Testing regex predicate...");
        
        Predicate<String, Person2> regexPredicate = Predicates.regex("name", "^[AB].*");
        Collection<Person2> regexResults = personMap.values(regexPredicate);
        
        boolean regexSuccess = regexResults.size() == 2; // Alice and Bob
        if (regexSuccess) {
            System.out.println("✓ Regex predicate query works correctly");
        } else {
            System.out.println("✗ Regex predicate query failed. Expected 2 people, got: " + regexResults.size());
        }
        recordTestResult("Predicate-Regex", regexSuccess, 
                            "Regex predicate query test. Expected 2 people, Got: " + regexResults.size());
    }
    
    public void testParameterizedSqlQueries() {
        System.out.println("\n=== Testing Parameterized SQL Queries ===");
        
        // Test parameterized query
        System.out.println("Testing parameterized query...");
        
        Map<String, Object> params = new HashMap<>();
        params.put("min_age", 30);
        params.put("is_active", true);
        
        try (SqlResult result = sqlService.execute(
                "SELECT * FROM persons WHERE age > ? AND active = ?", 
                params.get("min_age"), params.get("is_active"))) {
            
            List<SqlRow> rows = new ArrayList<>();
            result.iterator().forEachRemaining(rows::add);
            
            boolean paramQueryWorks = rows.size() == 1; // Only Alice
            if (paramQueryWorks) {
                System.out.println("✓ Parameterized query works correctly");
            } else {
                System.out.println("✗ Parameterized query failed. Expected 1 row, got: " + rows.size());
            }
            recordTestResult("SqlQuery-Parameterized", paramQueryWorks, 
                                "Parameterized query test. Expected 1 row, got: " + rows.size());
        }
    }
}

// Additional class needed for tests
class Department2 implements Serializable {
    private String id;
    private String name;
    private String location;
    
    public Department2(String id, String name, String location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}

// Person class for testing
class Person2 implements Serializable {
    private String name;
    private int age;
    private boolean active;
    private String departmentId; // Added for join tests
    
    public Person2(String name, int age, boolean active, String departmentId) {
        this.name = name;
        this.age = age;
        this.active = active;
        this.departmentId = departmentId;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    public boolean isActive() { return active; }
    public String getDepartmentId() { return departmentId; }
    
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setActive(boolean active) { this.active = active; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + ", active=" + active + ", departmentId='" + departmentId + "'}";
    }
}
