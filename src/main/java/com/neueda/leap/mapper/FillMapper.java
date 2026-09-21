package com.neueda.leap.mapper;

import com.neueda.leap.Fill;
import com.neueda.leap.enums.FillStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface FillMapper {

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE fill_id = #{fillId}")
    Fill selectFillById(@Param("fillId") UUID fillId);

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE order_id = #{orderId} ORDER BY executed_at ASC")
    List<Fill> selectFillsByOrderId(@Param("orderId") UUID orderId);

    @Select("SELECT COUNT(*) FROM trading.fills WHERE order_id = #{orderId}")
    int countFillsByOrderId(@Param("orderId") UUID orderId);

    @Select("SELECT fill_id as fillId, order_id as orderId, price, quantity, status, executed_at as executedAt FROM trading.fills WHERE status = #{status} LIMIT #{limit} OFFSET #{offset}")
    List<Fill> selectFillsByStatus(@Param("status") FillStatus status, @Param("limit") int limit, @Param("offset") int offset);
}
