package com.arbergashi.charts.springdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the local Spring Boot chart workbench.
 *
 * <p>The application uses the server bridge for rendering and does not require
 * a desktop session. Its default address is {@code 127.0.0.1:8080}.</p>
 */
@SpringBootApplication
public class ChartWorkbenchApplication {

    /**
     * Starts the application using Spring Boot command-line configuration.
     *
     * @param args application arguments, such as {@code --server.port=8081}
     */
    public static void main(String[] args) {
        SpringApplication.run(ChartWorkbenchApplication.class, args);
    }
}
