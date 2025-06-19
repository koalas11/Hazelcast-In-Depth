package com.sanvito_damiano.hazelcast.tests.data_structures;

import java.io.Serializable;

// Person class for testing
public class Person implements Serializable {
    private String name;
    private int age;
    private boolean active;
    
    public Person(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    public boolean isActive() { return active; }
    
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setActive(boolean active) { this.active = active; }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + ", active=" + active + '}';
    }
}
