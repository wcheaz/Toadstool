package com.neueda.leap.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface AdminAuditWriteMapper {

    @Insert("INSERT INTO trading.admin_access_events (admin_user_id, admin_email, client_id, action, details) "
            + "VALUES (#{adminUserId}, #{adminEmail}, #{clientId}, #{action}, CAST(#{details} AS jsonb))")
    void insertAccessEvent(@Param("adminUserId") UUID adminUserId, @Param("adminEmail") String adminEmail,
                           @Param("clientId") UUID clientId, @Param("action") String action,
                           @Param("details") String details);
}
