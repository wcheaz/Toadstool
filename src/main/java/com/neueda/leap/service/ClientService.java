package com.neueda.leap.service;

import com.neueda.leap.Client;
import com.neueda.leap.mapper.ClientMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Client operations
 */
@Service
public class ClientService {

    private final ClientMapper clientMapper;

    public ClientService(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    /**
     * Get client by ID
     */
    public Client getClientById(UUID clientId) {
        return clientMapper.selectClientById(clientId);
    }

    /**
     * Get client by email
     */
    public Client getClientByEmail(String email) {
        return clientMapper.selectClientByEmail(email);
    }

    /**
     * List all clients with pagination
     */
    public List<Client> listAllClients(int limit, int offset) {
        return clientMapper.selectAllClients(limit, offset);
    }

    /**
     * Count all clients
     */
    public int countAllClients() {
        return clientMapper.countClients();
    }

    /**
     * List clients filtered by status
     */
    public List<Client> listClientsByStatus(String status, int limit, int offset) {
        return clientMapper.selectClientsByStatus(status, limit, offset);
    }

    /**
     * Count clients by status
     */
    public int countClientsByStatus(String status) {
        return clientMapper.countClientsByStatus(status);
    }

    /**
     * Update client profile (display name)
     */
    public void updateClientProfile(UUID clientId, String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }
        if (displayName.length() > 120) {
            throw new IllegalArgumentException("Display name must not exceed 120 characters");
        }
        clientMapper.updateClient(clientId, displayName);
    }
}
