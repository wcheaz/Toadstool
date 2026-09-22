package com.neueda.leap;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Test configuration that ensures Flyway cleans and migrates the database before tests.
 */
@Configuration
public class TestDatabaseConfig {

    /**
     * Override the default Flyway bean to ensure clean() is called before migrate()
     */
    @Bean
    @Primary
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .validateOnMigrate(false)
                .outOfOrder(true)
                .load();
        
        // Clean existing schema to start fresh
        try {
            flyway.clean();
        } catch (Exception e) {
            // Ignore if schema doesn't exist yet (first run)
        }
        
        // Apply all migrations
        flyway.migrate();
        
        return flyway;
    }
}

