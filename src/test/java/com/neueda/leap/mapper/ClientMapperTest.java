package com.neueda.leap.mapper;

import com.neueda.leap.Client;
import com.neueda.leap.enums.ClientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ClientMapperTest {

    @Autowired
    private ClientMapper clientMapper;

    @Test
    public void testCountClients() {
        int count = clientMapper.countClients();
        assertTrue(count > 0);
        assertEquals(25, count); // Test data inserts 25 clients
    }

    @Test
    public void testSelectClientByEmail() {
        Client client = clientMapper.selectClientByEmail("alice.johnson@example.com");
        assertNotNull(client);
        assertEquals("alice.johnson@example.com", client.getEmail());
        assertEquals("Alice Johnson", client.getDisplayName());
        assertEquals(ClientStatus.ACTIVE, client.getStatus());
    }

    @Test
    public void testSelectClientByEmailNotFound() {
        Client client = clientMapper.selectClientByEmail("nonexistent@example.com");
        assertNull(client);
    }

    @Test
    public void testSelectClientById() {
        // First get a client by email to get its ID
        Client clientByEmail = clientMapper.selectClientByEmail("bob.smith@example.com");
        assertNotNull(clientByEmail);

        // Then fetch by ID
        Client clientById = clientMapper.selectClientById(clientByEmail.getClientId());
        assertNotNull(clientById);
        assertEquals("bob.smith@example.com", clientById.getEmail());
        assertEquals("Bob Smith", clientById.getDisplayName());
    }

    @Test
    public void testSelectAllClients() {
        List<Client> clients = clientMapper.selectAllClients(10, 0);
        assertNotNull(clients);
        assertFalse(clients.isEmpty());
        assertTrue(clients.size() <= 10);
        clients.forEach(c -> {
            assertNotNull(c.getClientId());
            assertNotNull(c.getEmail());
            assertNotNull(c.getDisplayName());
            assertTrue(c.getStatus() == ClientStatus.ACTIVE || c.getStatus() == ClientStatus.SUSPENDED || c.getStatus() == ClientStatus.CLOSED);
        });
    }

    @Test
    public void testClientPagination() {
        List<Client> page1 = clientMapper.selectAllClients(5, 0);
        List<Client> page2 = clientMapper.selectAllClients(5, 5);
        
        assertNotNull(page1);
        assertNotNull(page2);
        assertEquals(5, page1.size());
        assertEquals(5, page2.size());
        
        // Pages should be different
        assertNotEquals(page1.get(0).getClientId(), page2.get(0).getClientId());
    }

    @Test
    public void testClientHasTimestamps() {
        Client client = clientMapper.selectClientByEmail("carol.white@example.com");
        assertNotNull(client);
        assertNotNull(client.getCreatedAt());
        assertNotNull(client.getUpdatedAt());
    }

    @Test
    public void testAllTestClientsExist() {
        String[] testEmails = {
            "alice.johnson@example.com",
            "bob.smith@example.com",
            "carol.white@example.com",
            "david.brown@example.com",
            "emma.davis@example.com"
        };

        for (String email : testEmails) {
            Client client = clientMapper.selectClientByEmail(email);
            assertNotNull(client, "Client with email " + email + " should exist");
            assertEquals(ClientStatus.ACTIVE, client.getStatus());
        }
    }
}
