package com.neueda.leap;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;

/**
 * Test execution listener that ensures Flyway cleans and migrates the database
 * before the first test method runs in the test suite.
 * 
 * This is automatically discovered and loaded by Spring's test framework.
 */
public class FlywayTestExecutionListener extends AbstractTestExecutionListener {

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
            DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);
            
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
                // Ignore if schema doesn't exist yet
            }
            
            // Run migrations
            flyway.migrate();
            
            initialized = true;
        }
    }
}
