package com.focus.springbootreact.payroll;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


// THIS IS RUN AFTER BOOT BECASUE IT IMPLEMENTS CommandLineRunner
@Component
public class DatabaseLoader implements CommandLineRunner {

    private final EmployeeRepository repository;

    @Autowired
    public DatabaseLoader(EmployeeRepository repository) {
        this.repository = repository;
    }

    // THIS IS RUN AFTER BOOT BECASUE IT IMPLEMENTS CommandLineRunner
    @Override
    public void run(String... strings) throws Exception {
        this.repository.save(new Employee("Frodo", "Baggins", "ring bearer"));
    }
}
