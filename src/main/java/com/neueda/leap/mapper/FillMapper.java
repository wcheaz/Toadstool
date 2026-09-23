package com.neueda.leap.mapper;

import com.neueda.leap.Fill;
import com.neueda.leap.enums.FillStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import java.util.List;
import java.util.UUID;

@Mapper
public interface FillMapper {

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE fill_id = #{fillId}")
    Fill selectFillById(@Param("fillId") UUID fillId);

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE order_id = #{orderId} ORDER BY executed_at ASC")
    List<Fill> selectFillsByOrderId(@Param("orderId") UUID orderId);

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE order_id = #{orderId} LIMIT #{limit} OFFSET #{offset} ORDER BY executed_at DESC")
    List<Fill> selectFillsByOrderIdPaginated(@Param("orderId") UUID orderId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.fills WHERE order_id = #{orderId}")
    int countFillsByOrderId(@Param("orderId") UUID orderId);

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE status = #{status} LIMIT #{limit} OFFSET #{offset}")
    List<Fill> selectFillsByStatus(@Param("status") FillStatus status, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT f.fill_id as fillId, f.order_id as orderId, f.price, f.quantity, f.status, f.executed_at as executedAt " +
            "FROM trading.fills f " +
            "JOIN trading.orders o ON f.order_id = o.order_id " +
            "WHERE o.account_id = #{accountId} " +
            "ORDER BY f.executed_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Fill> selectFillsByAccountId(@Param("accountId") UUID accountId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.fills f JOIN trading.orders o ON f.order_id = o.order_id WHERE o.account_id = #{accountId}")
    int countFillsByAccountId(@Param("accountId") UUID accountId);

    @Insert("INSERT INTO trading.fills (order_id, price, quantity, status) VALUES (#{orderId}, #{price}, #{quantity}, #{status})")
    void insertFill(@Param("orderId") UUID orderId, @Param("price") String price, @Param("quantity") String quantity, @Param("status") String status);
}
