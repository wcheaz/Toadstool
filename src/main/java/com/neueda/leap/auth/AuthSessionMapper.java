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
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "issuedAt", column = "issued_at"),
            @Result(property = "accessTokenExpiresAt", column = "access_token_expires_at"),
            @Result(property = "refreshTokenHash", column = "refresh_token_hash"),
            @Result(property = "refreshTokenExpiresAt", column = "refresh_token_expires_at"),
            @Result(property = "revokedAt", column = "revoked_at"),
            @Result(property = "lastUsedAt", column = "last_used_at")
    })
    @Select("SELECT session_key_id, credential_id, client_id, issued_at, access_token_expires_at, refresh_token_hash, refresh_token_expires_at, revoked_at, last_used_at " +
            "FROM trading.auth_sessions WHERE session_key_id = #{sessionKeyId} AND revoked_at IS NULL")
    AuthSession selectActiveBySessionKeyId(@Param("sessionKeyId") UUID sessionKeyId);

    @Insert("INSERT INTO trading.auth_sessions (session_key_id, credential_id, client_id, issued_at, access_token_expires_at, refresh_token_hash, refresh_token_expires_at, last_used_at) " +
            "VALUES (#{sessionKeyId}, #{credentialId}, #{clientId}, #{issuedAt}, #{accessTokenExpiresAt}, #{refreshTokenHash}, #{refreshTokenExpiresAt}, #{lastUsedAt})")
    void insertSession(@Param("sessionKeyId") UUID sessionKeyId, @Param("credentialId") UUID credentialId,
                       @Param("clientId") UUID clientId, @Param("issuedAt") OffsetDateTime issuedAt,
                       @Param("accessTokenExpiresAt") OffsetDateTime accessTokenExpiresAt,
                       @Param("refreshTokenHash") String refreshTokenHash,
                       @Param("refreshTokenExpiresAt") OffsetDateTime refreshTokenExpiresAt,
                       @Param("lastUsedAt") OffsetDateTime lastUsedAt);

    @Results(value = {
            @Result(property = "sessionKeyId", column = "session_key_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "credentialId", column = "credential_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "issuedAt", column = "issued_at"),
            @Result(property = "accessTokenExpiresAt", column = "access_token_expires_at"),
            @Result(property = "refreshTokenHash", column = "refresh_token_hash"),
            @Result(property = "refreshTokenExpiresAt", column = "refresh_token_expires_at"),
            @Result(property = "revokedAt", column = "revoked_at"),
            @Result(property = "lastUsedAt", column = "last_used_at")
    })
    @Select("SELECT session_key_id, credential_id, client_id, issued_at, access_token_expires_at, refresh_token_hash, refresh_token_expires_at, revoked_at, last_used_at " +
            "FROM trading.auth_sessions WHERE refresh_token_hash = #{refreshTokenHash} AND revoked_at IS NULL")
    AuthSession selectActiveByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);

    @Update("UPDATE trading.auth_sessions SET access_token_expires_at = #{accessTokenExpiresAt}, refresh_token_hash = #{refreshTokenHash}, refresh_token_expires_at = #{refreshTokenExpiresAt}, last_used_at = NOW() " +
            "WHERE revoked_at IS NULL AND session_key_id = #{sessionKeyId}")
    int rotateSessionTokens(@Param("sessionKeyId") UUID sessionKeyId,
                            @Param("accessTokenExpiresAt") OffsetDateTime accessTokenExpiresAt,
                            @Param("refreshTokenHash") String refreshTokenHash,
                            @Param("refreshTokenExpiresAt") OffsetDateTime refreshTokenExpiresAt);

    @Update("UPDATE trading.auth_sessions SET revoked_at = NOW(), last_used_at = NOW() " +
            "WHERE revoked_at IS NULL AND credential_id = #{credentialId}")
    int revokeActiveSessionsByCredentialId(@Param("credentialId") UUID credentialId);

    @Update("UPDATE trading.auth_sessions SET revoked_at = NOW(), last_used_at = NOW() " +
            "WHERE revoked_at IS NULL AND access_token_expires_at <= #{now}")
    int revokeExpiredSessions(@Param("now") OffsetDateTime now);

    @Update("UPDATE trading.auth_sessions SET revoked_at = NOW(), last_used_at = NOW() " +
            "WHERE revoked_at IS NULL AND session_key_id = #{sessionKeyId}")
    int revokeSession(@Param("sessionKeyId") UUID sessionKeyId);

    @Update("UPDATE trading.auth_sessions SET access_token_expires_at = #{accessTokenExpiresAt}, last_used_at = NOW() " +
            "WHERE revoked_at IS NULL AND session_key_id = #{sessionKeyId}")
    int extendSession(@Param("sessionKeyId") UUID sessionKeyId,
                      @Param("accessTokenExpiresAt") OffsetDateTime accessTokenExpiresAt);
}
