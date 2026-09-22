package com.neueda.leap;

import org.springframework.context.annotation.Configuration;

/**
 * Empty test configuration.
 * Database initialization is now handled by FlywayTestExecutionListener
 * to ensure it runs at the correct time in the Spring initialization lifecycle.
 */
@Configuration
public class TestDatabaseConfig {
    // Configuration moved to FlywayTestExecutionListener
}


