package com.neueda.leap.controller;

import com.neueda.leap.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static MockMvc buildMockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    static String isoTimestamp(OffsetDateTime value) {
        return value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    static void authenticateClient(UUID clientId, UUID accountId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(clientId.toString(), "client-user", "client@example.com", clientId, accountId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        ));
    }

    static void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID().toString(), "admin-user", "admin@example.com", null, null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
