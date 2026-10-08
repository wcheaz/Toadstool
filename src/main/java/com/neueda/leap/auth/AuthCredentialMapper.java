package com.neueda.leap.auth;

import com.neueda.leap.mapper.UuidTypeHandler;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

@Mapper
public interface AuthCredentialMapper {

    @Results(id = "authCredentialResultMap", value = {
            @Result(property = "credentialId", column = "credential_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "username", column = "username"),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "lastLoginAt", column = "last_login_at")
    })
    @Select("SELECT credential_id, account_id, username, password_hash, " +
            "created_at, updated_at, last_login_at " +
            "FROM trading.account_credentials WHERE username = #{username}")
    AuthCredential selectByUsername(@Param("username") String username);

    @Results(value = {
            @Result(property = "credentialId", column = "credential_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "username", column = "username"),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "lastLoginAt", column = "last_login_at")
    })
    @Select("SELECT credential_id, account_id, username, password_hash, " +
            "created_at, updated_at, last_login_at " +
            "FROM trading.account_credentials WHERE account_id = #{accountId}")
    AuthCredential selectByAccountId(@Param("accountId") UUID accountId);

    @Insert("INSERT INTO trading.account_credentials (credential_id, account_id, username, password_hash) " +
            "VALUES (#{credentialId}, #{accountId}, #{username}, #{passwordHash})")
    void insertCredential(@Param("credentialId") UUID credentialId, @Param("accountId") UUID accountId,
                          @Param("username") String username, @Param("passwordHash") String passwordHash);

    @Update("UPDATE trading.account_credentials SET last_login_at = NOW(), updated_at = NOW() WHERE credential_id = #{credentialId}")
    void markLastLogin(@Param("credentialId") UUID credentialId);
}
