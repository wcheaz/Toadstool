package com.neueda.leap.auth;

import com.neueda.leap.mapper.UuidTypeHandler;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper
public interface AuthSessionMapper {

    @Results(id = "authSessionResultMap", value = {
            @Result(property = "sessionKeyId", column = "session_key_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "credentialId", column = "credential_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "issuedAt", column = "issued_at"),
            @Result(property = "accessTokenExpiresAt", column = "access_token_expires_at"),
            @Result(property = "revokedAt", column = "revoked_at"),
            @Result(property = "lastUsedAt", column = "last_used_at")
    })
    @Select("SELECT session_key_id, credential_id, account_id, issued_at, access_token_expires_at, revoked_at, last_used_at " +
            "FROM trading.auth_sessions WHERE session_key_id = #{sessionKeyId}")
    AuthSession selectBySessionKeyId(@Param("sessionKeyId") UUID sessionKeyId);

    @Insert("INSERT INTO trading.auth_sessions (session_key_id, credential_id, account_id, issued_at, access_token_expires_at, last_used_at) " +
            "VALUES (#{sessionKeyId}, #{credentialId}, #{accountId}, #{issuedAt}, #{accessTokenExpiresAt}, #{lastUsedAt})")
    void insertSession(@Param("sessionKeyId") UUID sessionKeyId, @Param("credentialId") UUID credentialId,
                       @Param("accountId") UUID accountId, @Param("issuedAt") OffsetDateTime issuedAt,
                       @Param("accessTokenExpiresAt") OffsetDateTime accessTokenExpiresAt, @Param("lastUsedAt") OffsetDateTime lastUsedAt);

    @Update("UPDATE trading.auth_sessions SET revoked_at = NOW(), last_used_at = NOW() WHERE session_key_id = #{sessionKeyId} AND revoked_at IS NULL")
    int revokeSession(@Param("sessionKeyId") UUID sessionKeyId);
}
