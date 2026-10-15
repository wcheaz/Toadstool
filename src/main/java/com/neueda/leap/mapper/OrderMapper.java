package com.neueda.leap.mapper;

import com.neueda.leap.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OrderMapper {

    @Select("SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt, COALESCE((SELECT UPPER(f.status) FROM trading.fills f WHERE f.order_id = trading.orders.order_id ORDER BY f.executed_at DESC LIMIT 1), 'SUBMITTED') as status FROM trading.orders WHERE order_id = #{orderId}")
    Order selectOrderById(@Param("orderId") UUID orderId);

    @Select("SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt, COALESCE((SELECT UPPER(f.status) FROM trading.fills f WHERE f.order_id = trading.orders.order_id ORDER BY f.executed_at DESC LIMIT 1), 'SUBMITTED') as status FROM trading.orders WHERE account_id = #{accountId} ORDER BY submitted_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Order> selectOrdersByAccountId(@Param("accountId") UUID accountId, @Param("limit") int limit, @Param("offset") int offset);

        @Select({"<script>",
            "SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt,",
            "COALESCE((SELECT UPPER(f.status) FROM trading.fills f WHERE f.order_id = trading.orders.order_id ORDER BY f.executed_at DESC LIMIT 1), 'SUBMITTED') as status",
            "FROM trading.orders",
            "<where>",
            "<if test='fromSubmittedAt != null'>submitted_at &gt;= #{fromSubmittedAt}</if>",
            "<if test='toSubmittedAt != null'>AND submitted_at &lt; #{toSubmittedAt}</if>",
            "</where>",
            "ORDER BY submitted_at DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
        List<Order> selectAllOrders(@Param("fromSubmittedAt") java.time.OffsetDateTime fromSubmittedAt,
                    @Param("toSubmittedAt") java.time.OffsetDateTime toSubmittedAt,
                    @Param("limit") int limit,
                    @Param("offset") int offset);

    @Select("SELECT order_id as orderId, account_id as accountId, instrument_id as instrumentId, side, quantity, idempotency_key as idempotencyKey, submitted_at as submittedAt, COALESCE((SELECT UPPER(f.status) FROM trading.fills f WHERE f.order_id = trading.orders.order_id ORDER BY f.executed_at DESC LIMIT 1), 'SUBMITTED') as status FROM trading.orders WHERE account_id = #{accountId} AND idempotency_key = #{idempotencyKey}")
    Order selectOrderByIdempotencyKey(@Param("accountId") UUID accountId, @Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT COUNT(*) FROM trading.orders WHERE account_id = #{accountId}")
    int countOrdersByAccountId(@Param("accountId") UUID accountId);

        @Select({"<script>",
            "SELECT COUNT(*) FROM trading.orders",
            "<where>",
            "<if test='fromSubmittedAt != null'>submitted_at &gt;= #{fromSubmittedAt}</if>",
            "<if test='toSubmittedAt != null'>AND submitted_at &lt; #{toSubmittedAt}</if>",
            "</where>",
            "</script>"})
        int countAllOrders(@Param("fromSubmittedAt") java.time.OffsetDateTime fromSubmittedAt,
                   @Param("toSubmittedAt") java.time.OffsetDateTime toSubmittedAt);

    @Insert("INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key) VALUES (#{accountId}, #{instrumentId}, #{side}, CAST(#{quantity} AS numeric), #{idempotencyKey})")
    void insertOrder(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId, 
                     @Param("side") String side, @Param("quantity") String quantity, @Param("idempotencyKey") String idempotencyKey);
}
