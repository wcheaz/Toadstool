package com.neueda.leap.config;

import org.junit.jupiter.api.Test;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.config.parameterstore.ParameterStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.SsmException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParameterStoreConfigurationTest {

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({AwsAutoConfiguration.class, CredentialsProviderAutoConfiguration.class,
            RegionProviderAutoConfiguration.class, ParameterStoreAutoConfiguration.class})
    static class TestConfiguration {
    }

    private SpringApplication application(SsmClient client) {
        SpringApplication application = new SpringApplication(TestConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(Map.of(
                "spring.config.location", "file:src/main/resources/",
                "spring.main.banner-mode", "off"));
        application.addBootstrapRegistryInitializer(registry ->
                registry.register(SsmClient.class, context -> client));
        return application;
    }

    private Parameter parameter(String name, String value) {
        return Parameter.builder().name("/toadstool/prod/" + name).value(value).build();
    }

    @Test
    void awsProfileLoadsBothDataSourcesAndUppercaseSettingsAcrossPages() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class)))
                .thenReturn(GetParametersByPathResponse.builder().parameters(
                        parameter("DB_USERNAME", "ssm-user"),
                        parameter("DB_PASSWORD", "test-only-password"),
                        parameter("SPRING_DATASOURCE_URL", "jdbc:postgresql://rds.example/trading?sslmode=verify-full"))
                        .nextToken("next-page").build())
                .thenReturn(GetParametersByPathResponse.builder().parameters(
                        parameter("REPORTING_DB_URL", "jdbc:postgresql://rds.example/reporting?sslmode=verify-full"),
                        parameter("SERVER_PORT", "8090"),
                        parameter("FAUXNANCE_API_KEY", "test-only-key")).build());

        try (var context = application(client).run("--spring.profiles.active=aws")) {
            var environment = context.getEnvironment();
            assertEquals("ssm-user", environment.getProperty("spring.datasource.username"));
            assertEquals("test-only-password", environment.getProperty("spring.datasource.password"));
            assertEquals("jdbc:postgresql://rds.example/trading?sslmode=verify-full",
                    environment.getProperty("spring.datasource.url"));
            assertEquals("jdbc:postgresql://rds.example/reporting?sslmode=verify-full",
                    environment.getProperty("reporting.datasource.url"));
            assertEquals("test-only-password", environment.getProperty("reporting.datasource.password"));
            assertEquals("8090", environment.getProperty("server.port"));
            assertEquals("test-only-key", environment.getProperty("fauxnance.apiKey"));
        }
        verify(client).getParametersByPath(GetParametersByPathRequest.builder()
                .path("/toadstool/prod/").recursive(true).withDecryption(true).build());
        verify(client).getParametersByPath(GetParametersByPathRequest.builder()
                .path("/toadstool/prod/").recursive(true).withDecryption(true).nextToken("next-page").build());
    }

    @Test
    void localProfileDoesNotFetchParameters() {
        SsmClient client = mock(SsmClient.class);
        try (var context = application(client).run("--spring.profiles.active=local",
                "--DB_USERNAME=local-user", "--DB_PASSWORD=local-test-password")) {
            assertEquals("local-user", context.getEnvironment().getProperty("spring.datasource.username"));
            assertTrue(context.getBeansOfType(SsmClient.class).isEmpty());
        }
        verifyNoInteractions(client);
    }

    @Test
    void deniedParameterStoreAccessFailsStartup() {
        SsmClient client = mock(SsmClient.class);
        when(client.getParametersByPath(any(GetParametersByPathRequest.class)))
                .thenThrow(SsmException.builder().message("AccessDenied test").statusCode(403).build());
        assertThrows(RuntimeException.class, () -> application(client).run("--spring.profiles.active=aws"));
    }
}
