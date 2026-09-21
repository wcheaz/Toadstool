package com.neueda.leap.mapper;

import com.neueda.leap.Account;
import com.neueda.leap.enums.AccountStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AccountMapper {

    @Select("SELECT account_id as accountId, client_id as clientId, status, opened_at as openedAt FROM trading.accounts WHERE account_id = #{accountId}")
    Account selectAccountById(@Param("accountId") UUID accountId);

    @Select("SELECT account_id as accountId, client_id as clientId, status, opened_at as openedAt FROM trading.accounts WHERE client_id = #{clientId} LIMIT #{limit} OFFSET #{offset}")
    List<Account> selectAccountsByClientId(@Param("clientId") UUID clientId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.accounts WHERE client_id = #{clientId}")
    int countAccountsByClientId(@Param("clientId") UUID clientId);

    @Select("SELECT account_id as accountId, client_id as clientId, status, opened_at as openedAt FROM trading.accounts WHERE status = #{status} LIMIT #{limit} OFFSET #{offset}")
    List<Account> selectAccountsByStatus(@Param("status") AccountStatus status, @Param("limit") int limit, @Param("offset") int offset);
}
