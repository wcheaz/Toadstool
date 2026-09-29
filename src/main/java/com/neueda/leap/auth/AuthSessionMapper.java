package com.neueda.leap.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper
public interface AuthSessionMapper {

    @Select("SELECT session_key_id, credential_id, account_id, " +
            "refresh_token_hash, issued_at, access_token_expires_at, " +
            "refresh_token_expires_at, revoked_at, last_used_at " +
            "FROM trading.auth_sessions WHERE session_key_id = #{sessionKeyId}")
    AuthSession selectBySessionKeyId(@Param("sessionKeyId") UUID sessionKeyId);

    @Select("SELECT session_key_id, credential_id, account_id, " +
            "refresh_token_hash, issued_at, access_token_expires_at, " +
            "refresh_token_expires_at, revoked_at, last_used_at " +
            "FROM trading.auth_sessions WHERE refresh_token_hash = #{refreshTokenHash} " +
            "AND revoked_at IS NULL AND refresh_token_expires_at > NOW()")
    AuthSession selectActiveByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);

    @Insert("INSERT INTO trading.auth_sessions (session_key_id, credential_id, account_id, refresh_token_hash, " +
            "issued_at, access_token_expires_at, refresh_token_expires_at, last_used_at) " +
            "VALUES (#{sessionKeyId}, #{credentialId}, #{accountId}, #{refreshTokenHash}, #{issuedAt}, " +
            "#{accessTokenExpiresAt}, #{refreshTokenExpiresAt}, #{lastUsedAt})")
    void insertSession(@Param("sessionKeyId") UUID sessionKeyId, @Param("credentialId") UUID credentialId,
                       @Param("accountId") UUID accountId, @Param("refreshTokenHash") String refreshTokenHash,
                       @Param("issuedAt") OffsetDateTime issuedAt, @Param("accessTokenExpiresAt") OffsetDateTime accessTokenExpiresAt,
                       @Param("refreshTokenExpiresAt") OffsetDateTime refreshTokenExpiresAt, @Param("lastUsedAt") OffsetDateTime lastUsedAt);

    @Update("UPDATE trading.auth_sessions SET revoked_at = NOW(), last_used_at = NOW() WHERE session_key_id = #{sessionKeyId} AND revoked_at IS NULL")
    int revokeSession(@Param("sessionKeyId") UUID sessionKeyId);

    @Update("UPDATE trading.auth_sessions SET last_used_at = NOW() WHERE session_key_id = #{sessionKeyId} AND revoked_at IS NULL")
    int touchSession(@Param("sessionKeyId") UUID sessionKeyId);
}
