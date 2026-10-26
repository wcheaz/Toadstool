package com.neueda.leap;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;

/**
 * Test execution listener that ensures pending Flyway migrations are applied
 * before the first test method runs in the suite.
 * 
 * This is automatically discovered and loaded by Spring's test framework.
 */
public class FlywayTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayTestExecutionListener.class);
    private static boolean initialized = false;

    @Override
    public int getOrder() {
        // Run before DependencyInjectionTestExecutionListener (order 2000)
        // so database is ready before Spring injects dependencies
        return 1000;
    }

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        // Only initialize once per test run
        if (!initialized) {
            if (testContext.getApplicationContext().getBeanNamesForType(DataSource.class).length == 0) {
                LOGGER.debug("Skipping Flyway test initialization for {} because no DataSource bean is present",
                        testContext.getTestClass().getName());
                return;
            }

            DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);
            
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migrations")
                    .cleanDisabled(true)
                    .validateOnMigrate(false)
                    .outOfOrder(true)
                    .load();
            
            // Apply pending migrations without deleting existing data.
            flyway.migrate();
            
            initialized = true;
        }
    }
}
