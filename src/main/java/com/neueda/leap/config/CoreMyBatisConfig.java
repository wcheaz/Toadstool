package com.neueda.leap.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = {"com.neueda.leap.mapper", "com.neueda.leap.auth"},
        sqlSessionFactoryRef = "coreSqlSessionFactory",
        sqlSessionTemplateRef = "coreSqlSessionTemplate"
)
public class CoreMyBatisConfig {

    @Bean(name = "coreSqlSessionFactory")
    public SqlSessionFactory coreSqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml")
        );

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        factoryBean.setTypeAliasesPackage("com.neueda.leap");
        factoryBean.setTypeHandlersPackage("com.neueda.leap.mapper");
        return factoryBean.getObject();
    }

    @Bean(name = "coreSqlSessionTemplate")
    public SqlSessionTemplate coreSqlSessionTemplate(
            @Qualifier("coreSqlSessionFactory") SqlSessionFactory coreSqlSessionFactory) {
        return new SqlSessionTemplate(coreSqlSessionFactory);
    }
}
