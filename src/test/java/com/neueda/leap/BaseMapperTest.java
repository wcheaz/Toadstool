package com.neueda.leap;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base test class that ensures the database is cleaned and migrated before tests run.
 * All test classes should extend this to get proper test database initialization.
 */
@SpringBootTest
@ContextConfiguration(classes = TestDatabaseConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class BaseMapperTest {
    // Empty base class - configuration is in class annotations
}
