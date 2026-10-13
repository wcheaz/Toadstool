package com.neueda.leap.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.UUID;

@Mapper
public interface CashEventMapper {
    @Insert("INSERT INTO trading.cash_events (account_id, order_id, event_type, amount, details) VALUES (#{accountId}, #{orderId}, #{eventType}, #{amount}, CAST(#{details} AS jsonb))")
    void insert(@Param("accountId") UUID accountId, @Param("orderId") UUID orderId,
                @Param("eventType") String eventType, @Param("amount") BigDecimal amount,
                @Param("details") String details);
}