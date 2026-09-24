package com.neueda.leap.mapper;

import com.neueda.leap.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis mapper for AdminUser
 */
@Mapper
public interface AdminUserMapper {

    @Select("SELECT admin_user_id as adminUserId, email, display_name as displayName, role, status, created_at as createdAt, updated_at as updatedAt " +
            "FROM trading.admin_users WHERE admin_user_id = #{adminUserId}")
    AdminUser selectAdminUserById(@Param("adminUserId") UUID adminUserId);

    @Select("SELECT admin_user_id as adminUserId, email, display_name as displayName, role, status, created_at as createdAt, updated_at as updatedAt " +
            "FROM trading.admin_users WHERE email = #{email}")
    AdminUser selectAdminUserByEmail(@Param("email") String email);

    @Select("SELECT admin_user_id as adminUserId, email, display_name as displayName, role, status, created_at as createdAt, updated_at as updatedAt " +
            "FROM trading.admin_users ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<AdminUser> selectAllAdminUsers(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.admin_users")
    int countAllAdminUsers();

    @Select("SELECT admin_user_id as adminUserId, email, display_name as displayName, role, status, created_at as createdAt, updated_at as updatedAt " +
            "FROM trading.admin_users WHERE role = #{role} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<AdminUser> selectAdminUsersByRole(@Param("role") String role, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.admin_users WHERE role = #{role}")
    int countAdminUsersByRole(@Param("role") String role);

    @Insert("INSERT INTO trading.admin_users (email, display_name, role, status) VALUES (#{email}, #{displayName}, #{role}, 'ACTIVE')")
    void insertAdminUser(@Param("email") String email, @Param("displayName") String displayName, @Param("role") String role);

    @Update("UPDATE trading.admin_users SET display_name = #{displayName}, role = #{role}, status = #{status}, updated_at = NOW() " +
            "WHERE admin_user_id = #{adminUserId}")
    void updateAdminUser(@Param("adminUserId") UUID adminUserId, @Param("displayName") String displayName,
                         @Param("role") String role, @Param("status") String status);
}
