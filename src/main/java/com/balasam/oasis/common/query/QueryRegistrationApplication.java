package com.balasam.oasis.common.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.balasam.oasis.common.query.example.ExampleQueryConfig;

@SpringBootApplication
@Import(ExampleQueryConfig.class)
public class QueryRegistrationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(QueryRegistrationApplication.class, args);
    }
}