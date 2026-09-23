package com.neueda.leap.mapper;

import com.neueda.leap.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OrderMapper {

    @Select("SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt FROM trading.orders WHERE order_id = #{orderId}")
    Order selectOrderById(@Param("orderId") UUID orderId);

    @Select("SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt FROM trading.orders WHERE account_id = #{accountId} ORDER BY submitted_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Order> selectOrdersByAccountId(@Param("accountId") UUID accountId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt FROM trading.orders WHERE account_id = #{accountId} AND idempotency_key = #{idempotencyKey}")
    Order selectOrderByIdempotencyKey(@Param("accountId") UUID accountId, @Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT COUNT(*) FROM trading.orders WHERE account_id = #{accountId}")
    int countOrdersByAccountId(@Param("accountId") UUID accountId);

    @Insert("INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key) VALUES (#{accountId}, #{instrumentId}, #{side}, #{quantity}, #{idempotencyKey})")
    void insertOrder(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId, 
                     @Param("side") String side, @Param("quantity") String quantity, @Param("idempotencyKey") String idempotencyKey);

    @Update("UPDATE trading.orders SET status = #{status} WHERE order_id = #{orderId}")
    void updateOrderStatus(@Param("orderId") UUID orderId, @Param("status") String status);
}
