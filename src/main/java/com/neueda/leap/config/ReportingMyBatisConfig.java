package com.neueda.leap.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = "com.neueda.leap.reporting.mapper",
        sqlSessionFactoryRef = "reportingSqlSessionFactory",
        sqlSessionTemplateRef = "reportingSqlSessionTemplate"
)
public class ReportingMyBatisConfig {

    @Bean(name = "reportingDataSourceProperties")
    @ConfigurationProperties("reporting.datasource")
    public DataSourceProperties reportingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "reportingDataSource")
    @ConfigurationProperties("reporting.datasource.hikari")
    public DataSource reportingDataSource(
            @Qualifier("reportingDataSourceProperties") DataSourceProperties reportingDataSourceProperties) {
        return reportingDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "reportingSqlSessionFactory")
    public SqlSessionFactory reportingSqlSessionFactory(
            @Qualifier("reportingDataSource") DataSource reportingDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(reportingDataSource);

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        factoryBean.setTypeAliasesPackage("com.neueda.leap");
        factoryBean.setTypeHandlersPackage("com.neueda.leap.mapper");
        return factoryBean.getObject();
    }

    @Bean(name = "reportingSqlSessionTemplate")
    public SqlSessionTemplate reportingSqlSessionTemplate(
            @Qualifier("reportingSqlSessionFactory") SqlSessionFactory reportingSqlSessionFactory) {
        return new SqlSessionTemplate(reportingSqlSessionFactory);
    }
}
