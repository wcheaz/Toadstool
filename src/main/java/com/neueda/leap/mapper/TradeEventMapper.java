package com.neueda.leap.mapper;

import com.neueda.leap.TradeEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis mapper for TradeEvent (audit trail)
 */
@Mapper
public interface TradeEventMapper {

    @Select("SELECT trade_event_id as tradeEventId, client_id as clientId, entity_type as entityType, " +
            "entity_id as entityId, action, occurred_at as occurredAt, details " +
            "FROM trading.trade_events WHERE trade_event_id = #{tradeEventId}")
    TradeEvent selectTradeEventById(@Param("tradeEventId") UUID tradeEventId);

    @Select("SELECT trade_event_id as tradeEventId, client_id as clientId, entity_type as entityType, " +
            "entity_id as entityId, action, occurred_at as occurredAt, details " +
            "FROM trading.trade_events WHERE entity_id = #{entityId} " +
            "ORDER BY occurred_at ASC")
    List<TradeEvent> selectTradeEventsByEntity(@Param("entityId") UUID entityId);

    @Select("SELECT trade_event_id as tradeEventId, client_id as clientId, entity_type as entityType, " +
            "entity_id as entityId, action, occurred_at as occurredAt, details " +
            "FROM trading.trade_events WHERE client_id = #{clientId} " +
            "ORDER BY occurred_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<TradeEvent> selectTradeEventsByClientId(@Param("clientId") UUID clientId, 
                                                   @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.trade_events WHERE client_id = #{clientId}")
    int countTradeEventsByClientId(@Param("clientId") UUID clientId);

    @Insert("INSERT INTO trading.trade_events (client_id, entity_type, entity_id, action, details) " +
            "VALUES (#{clientId}, #{entityType}, #{entityId}, #{action}, #{details}::jsonb)")
    void insertTradeEvent(@Param("clientId") UUID clientId, @Param("entityType") String entityType,
                          @Param("entityId") UUID entityId, @Param("action") String action, @Param("details") String details);
}
