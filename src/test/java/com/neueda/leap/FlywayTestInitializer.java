package com.neueda.leap;

import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;

/**
 * Initializes the test database by running Flyway clean and migrate.
 * This runs before the Spring context is fully initialized, ensuring
 * a fresh database before any beans are created or tests run.
 */
public class FlywayTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Get the datasource
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        
        // Configure and run Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .validateOnMigrate(false)
                .outOfOrder(true)
                .load();
        
        // Clean existing schema
        try {
            flyway.clean();
        } catch (Exception e) {
            // Schema doesn't exist yet, which is fine
        }
        
        // Run all migrations
        flyway.migrate();
    }
}
