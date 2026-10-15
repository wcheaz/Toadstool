package com.neueda.leap.auth;

import com.neueda.leap.mapper.UuidTypeHandler;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/**
 * Read-only access to admin credentials. Admins are provisioned manually in the database,
 * so this mapper intentionally has no insert/update/delete statements.
 */
@Mapper
public interface AdminCredentialMapper {

    @Results(id = "adminCredentialResultMap", value = {
            @Result(property = "credentialId", column = "credential_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "adminUserId", column = "admin_user_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "username", column = "username"),
            @Result(property = "passwordHash", column = "password_hash"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT credential_id, admin_user_id, username, password_hash, created_at, updated_at " +
            "FROM trading.admin_credentials WHERE username = #{username}")
    AdminCredential selectByUsername(@Param("username") String username);
}
