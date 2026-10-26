package com.neueda.leap.mapper;

import com.neueda.leap.Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ClientMapper {

    @Results(id = "clientResultMap", value = {
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "email", column = "email"),
            @Result(property = "displayName", column = "display_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT client_id, email, display_name, status, created_at, updated_at FROM trading.clients WHERE client_id = #{clientId}")
    Client selectClientById(@Param("clientId") UUID clientId);

    @Results(value = {
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "email", column = "email"),
            @Result(property = "displayName", column = "display_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT client_id, email, display_name, status, created_at, updated_at FROM trading.clients WHERE email = #{email}")
    Client selectClientByEmail(@Param("email") String email);

    @Results(value = {
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "email", column = "email"),
            @Result(property = "displayName", column = "display_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT client_id, email, display_name, status, created_at, updated_at FROM trading.clients ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Client> selectAllClients(@Param("limit") int limit, @Param("offset") int offset);

    @Results(value = {
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "email", column = "email"),
            @Result(property = "displayName", column = "display_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT client_id, email, display_name, status, created_at, updated_at FROM trading.clients WHERE status = #{status} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Client> selectClientsByStatus(@Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.clients")
    int countClients();

    @Select("SELECT COUNT(*) FROM trading.clients WHERE status = #{status}")
    int countClientsByStatus(@Param("status") String status);

    @Insert("INSERT INTO trading.clients (client_id, email, display_name, status) " +
            "VALUES (#{clientId}, #{email}, #{displayName}, #{status})")
    void insertClient(@Param("clientId") UUID clientId, @Param("email") String email,
                      @Param("displayName") String displayName, @Param("status") String status);

    @Update("UPDATE trading.clients SET display_name = #{displayName}, updated_at = NOW() WHERE client_id = #{clientId}")
    void updateClient(@Param("clientId") UUID clientId, @Param("displayName") String displayName);
}
