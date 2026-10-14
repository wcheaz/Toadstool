package com.neueda.leap;

import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;

/**
 * Initializes the test database by applying pending Flyway migrations.
 * Existing schemas and data are preserved between test runs.
 */
public class FlywayTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Get the datasource
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        
        // Configure and run Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migrations")
                .cleanDisabled(true)
                .validateOnMigrate(false)
                .outOfOrder(true)
                .load();
        
        // Apply pending migrations without deleting existing data.
        flyway.migrate();
    }
}
