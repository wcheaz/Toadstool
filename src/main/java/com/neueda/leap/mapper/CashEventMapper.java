package com.neueda.leap.mapper;

import com.neueda.leap.dto.CashEventResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface CashEventMapper {
    @Insert("INSERT INTO trading.cash_events (account_id, order_id, event_type, amount, details) VALUES (#{accountId}, #{orderId}, #{eventType}, #{amount}, CAST(#{details} AS jsonb))")
    void insert(@Param("accountId") UUID accountId, @Param("orderId") UUID orderId,
                @Param("eventType") String eventType, @Param("amount") BigDecimal amount,
                @Param("details") String details);

    @Select("SELECT cash_event_id, account_id, order_id, event_type, amount, occurred_at, details::text AS details " +
            "FROM trading.cash_events WHERE account_id = #{accountId} " +
            "ORDER BY occurred_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<CashEventResponse> selectByAccountId(@Param("accountId") UUID accountId,
                                              @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.cash_events WHERE account_id = #{accountId}")
    int countByAccountId(@Param("accountId") UUID accountId);
}
