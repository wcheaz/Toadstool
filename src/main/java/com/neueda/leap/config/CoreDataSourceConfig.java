package com.neueda.leap.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class CoreDataSourceConfig {

    @Primary
    @Bean(name = "coreDataSourceProperties")
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties coreDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(
            @Qualifier("coreDataSourceProperties") DataSourceProperties coreDataSourceProperties) {
        return coreDataSourceProperties.initializeDataSourceBuilder().build();
    }
}
