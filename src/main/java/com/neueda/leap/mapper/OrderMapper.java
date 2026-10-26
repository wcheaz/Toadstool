package com.neueda.leap.mapper;

import com.neueda.leap.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OrderMapper {

    /**
     * Order columns plus its execution outcome. Each order has at most one fill (ux_fills_order):
     * no fill = ACCEPTED (passed the trading rules, awaiting execution), Filled = FILLED, Failed = REJECTED.
     */
    String ORDER_COLUMNS = "o.order_id as orderId, o.account_id as accountId, o.instrument_id as instrumentId, o.side, o.quantity, "
            + "o.idempotency_key as idempotencyKey, o.submitted_at as submittedAt, o.order_type as orderType, o.limit_price as limitPrice, "
            + "CASE f.status WHEN 'Filled' THEN 'FILLED' WHEN 'Failed' THEN 'REJECTED' WHEN 'Pending' THEN 'PENDING' ELSE 'ACCEPTED' END as status, "
            + "CASE WHEN f.status = 'Filled' THEN f.price END as fillPrice, f.failure_reason as rejectionReason ";
    String ORDERS_WITH_OUTCOME = "FROM trading.orders o LEFT JOIN trading.fills f ON f.order_id = o.order_id ";

    @Select("SELECT " + ORDER_COLUMNS + ORDERS_WITH_OUTCOME + "WHERE o.order_id = #{orderId}")
    Order selectOrderById(@Param("orderId") UUID orderId);

    @Select("SELECT " + ORDER_COLUMNS + ORDERS_WITH_OUTCOME
            + "WHERE o.account_id = #{accountId} ORDER BY o.submitted_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Order> selectOrdersByAccountId(@Param("accountId") UUID accountId, @Param("limit") int limit, @Param("offset") int offset);

        @Select({"<script>",
            "SELECT " + ORDER_COLUMNS + ORDERS_WITH_OUTCOME,
            "<where>",
            "<if test='fromSubmittedAt != null'>o.submitted_at &gt;= #{fromSubmittedAt}</if>",
            "<if test='toSubmittedAt != null'>AND o.submitted_at &lt; #{toSubmittedAt}</if>",
            "</where>",
            "ORDER BY o.submitted_at DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
        List<Order> selectAllOrders(@Param("fromSubmittedAt") java.time.OffsetDateTime fromSubmittedAt,
                    @Param("toSubmittedAt") java.time.OffsetDateTime toSubmittedAt,
                    @Param("limit") int limit,
                    @Param("offset") int offset);

    @Select("SELECT " + ORDER_COLUMNS + ORDERS_WITH_OUTCOME
            + "WHERE o.account_id = #{accountId} AND o.idempotency_key = #{idempotencyKey}")
    Order selectOrderByIdempotencyKey(@Param("accountId") UUID accountId, @Param("idempotencyKey") String idempotencyKey);

    /** Orders still awaiting execution that were submitted before the given time, oldest first. */
    @Select("SELECT " + ORDER_COLUMNS + ORDERS_WITH_OUTCOME
            + "WHERE f.fill_id IS NULL AND o.submitted_at < #{submittedBefore} ORDER BY o.submitted_at LIMIT #{limit}")
    List<Order> selectUnexecutedOrders(@Param("submittedBefore") OffsetDateTime submittedBefore, @Param("limit") int limit);

    /** Quantity already committed to SELL orders that have not executed yet. */
    @Select("SELECT COALESCE(SUM(o.quantity), 0) FROM trading.orders o "
            + "WHERE o.account_id = #{accountId} AND o.instrument_id = #{instrumentId} AND o.side = 'SELL' "
            + "AND NOT EXISTS (SELECT 1 FROM trading.fills f WHERE f.order_id = o.order_id)")
    BigDecimal sumPendingSellQuantity(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId);

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

    @Insert("INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, order_type, limit_price) "
            + "VALUES (#{accountId}, #{instrumentId}, #{side}, CAST(#{quantity} AS numeric), #{idempotencyKey}, #{orderType}, #{limitPrice})")
    void insertOrder(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId,
                     @Param("side") String side, @Param("quantity") String quantity, @Param("idempotencyKey") String idempotencyKey,
                     @Param("orderType") String orderType, @Param("limitPrice") BigDecimal limitPrice);
}
