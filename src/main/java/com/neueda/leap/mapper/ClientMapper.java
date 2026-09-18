package com.neueda.leap.mapper;

import com.neueda.leap.Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ClientMapper {

    @Select("SELECT client_id as clientId, email, display_name as displayName, status, created_at as createdAt, updated_at as updatedAt FROM trading.clients WHERE client_id = #{clientId}")
    Client selectClientById(@Param("clientId") UUID clientId);

    @Select("SELECT client_id as clientId, email, display_name as displayName, status, created_at as createdAt, updated_at as updatedAt FROM trading.clients WHERE email = #{email}")
    Client selectClientByEmail(@Param("email") String email);

    @Select("SELECT client_id as clientId, email, display_name as displayName, status, created_at as createdAt, updated_at as updatedAt FROM trading.clients ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Client> selectAllClients(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.clients")
    int countClients();
}
