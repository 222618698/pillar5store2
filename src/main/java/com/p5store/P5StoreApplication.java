package com.p5store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class P5StoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(P5StoreApplication.class, args);
    }
}
