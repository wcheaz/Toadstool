package com.neueda.leap.mapper;

import com.neueda.leap.Account;
import com.neueda.leap.enums.AccountStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AccountMapper {

    @Results(id = "accountResultMap", value = {
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "accountName", column = "account_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "openedAt", column = "opened_at"),
            @Result(property = "balance", column = "balance")
    })
        @Select("SELECT account_id, client_id, account_name, status, opened_at, balance FROM trading.accounts WHERE account_id = #{accountId}")
    Account selectAccountById(@Param("accountId") UUID accountId);

    @Results(value = {
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "accountName", column = "account_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "openedAt", column = "opened_at"),
            @Result(property = "balance", column = "balance")
    })
        @Select("SELECT account_id, client_id, account_name, status, opened_at, balance FROM trading.accounts WHERE client_id = #{clientId} ORDER BY opened_at, account_id LIMIT #{limit} OFFSET #{offset}")
    List<Account> selectAccountsByClientId(@Param("clientId") UUID clientId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.accounts WHERE client_id = #{clientId}")
    int countAccountsByClientId(@Param("clientId") UUID clientId);

    @Results(value = {
            @Result(property = "accountId", column = "account_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "clientId", column = "client_id", typeHandler = UuidTypeHandler.class),
            @Result(property = "accountName", column = "account_name"),
            @Result(property = "status", column = "status"),
            @Result(property = "openedAt", column = "opened_at"),
            @Result(property = "balance", column = "balance")
    })
        @Select("SELECT account_id, client_id, account_name, status, opened_at, balance FROM trading.accounts WHERE status = #{status} LIMIT #{limit} OFFSET #{offset}")
    List<Account> selectAccountsByStatus(@Param("status") AccountStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO trading.accounts (client_id, status) VALUES (#{clientId}, #{status})")
    void insertAccount(@Param("clientId") UUID clientId, @Param("status") String status);

        @Insert("INSERT INTO trading.accounts (client_id, account_name, status) VALUES (#{clientId}, #{accountName}, #{status})")
        void insertAccountNamed(@Param("clientId") UUID clientId, @Param("accountName") String accountName, @Param("status") String status);

    @Insert("INSERT INTO trading.accounts (account_id, client_id, status) VALUES (#{accountId}, #{clientId}, #{status})")
    void insertAccountWithId(@Param("accountId") UUID accountId, @Param("clientId") UUID clientId, @Param("status") String status);
    @Update("UPDATE trading.accounts SET balance = #{balance} WHERE account_id = #{accountId}")
    void updateAccountBalance(@Param("accountId") UUID accountId, @Param("balance") BigDecimal balance);

        @Update("UPDATE trading.accounts SET balance = balance + #{delta} WHERE account_id = #{accountId} AND balance + #{delta} >= 0")
        int applyBalanceDelta(@Param("accountId") UUID accountId, @Param("delta") BigDecimal delta);

        @Update("UPDATE trading.accounts SET account_name = #{accountName} WHERE account_id = #{accountId}")
        int updateAccountName(@Param("accountId") UUID accountId, @Param("accountName") String accountName);
}
