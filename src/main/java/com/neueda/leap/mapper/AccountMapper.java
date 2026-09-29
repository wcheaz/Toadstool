package com.neueda.leap.mapper;

import com.neueda.leap.Account;
import com.neueda.leap.enums.AccountStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AccountMapper {

    @Results(id = "accountResultMap", value = {
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "status", column = "status"),
            @Result(property = "openedAt", column = "opened_at")
    })
    @Select("SELECT account_id, client_id, status, opened_at FROM trading.accounts WHERE account_id = #{accountId}")
    Account selectAccountById(@Param("accountId") UUID accountId);

    @Results(value = {
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "status", column = "status"),
            @Result(property = "openedAt", column = "opened_at")
    })
    @Select("SELECT account_id, client_id, status, opened_at FROM trading.accounts WHERE client_id = #{clientId} LIMIT #{limit} OFFSET #{offset}")
    List<Account> selectAccountsByClientId(@Param("clientId") UUID clientId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.accounts WHERE client_id = #{clientId}")
    int countAccountsByClientId(@Param("clientId") UUID clientId);

    @Results(value = {
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "status", column = "status"),
            @Result(property = "openedAt", column = "opened_at")
    })
    @Select("SELECT account_id, client_id, status, opened_at FROM trading.accounts WHERE status = #{status} LIMIT #{limit} OFFSET #{offset}")
    List<Account> selectAccountsByStatus(@Param("status") AccountStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO trading.accounts (client_id, status) VALUES (#{clientId}, #{status})")
    void insertAccount(@Param("clientId") UUID clientId, @Param("status") String status);

    @Insert("INSERT INTO trading.accounts (account_id, client_id, status) VALUES (#{accountId}, #{clientId}, #{status})")
    void insertAccountWithId(@Param("accountId") UUID accountId, @Param("clientId") UUID clientId, @Param("status") String status);
}
