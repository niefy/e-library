package com.example.elibrary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ELibraryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ELibraryServiceApplication.class, args);
    }
}
