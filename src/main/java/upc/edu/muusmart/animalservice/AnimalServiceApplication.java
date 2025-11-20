package upc.edu.muusmart.animalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Animal Management microservice.
 *
 * <p>This microservice handles the registration and retrieval of bovine data. It
 * leverages Spring Boot for rapid development and runs an embedded server.
 */
@SpringBootApplication
public class AnimalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnimalServiceApplication.class, args);
    }
}