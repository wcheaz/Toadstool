package com.neueda.leap.mapper;

import com.neueda.leap.Holdings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MyBatis mapper for Holdings persistence operations
 */
@Mapper
public interface HoldingsMapper {

    @Select("SELECT holding_id as holdingId, account_id as accountId, instrument_id as instrumentId, quantity, updated_at as updatedAt FROM trading.holdings WHERE holding_id = #{holdingId}")
    Holdings selectHoldingById(@Param("holdingId") UUID holdingId);

    @Select("SELECT holding_id as holdingId, account_id as accountId, instrument_id as instrumentId, quantity, updated_at as updatedAt FROM trading.holdings WHERE account_id = #{accountId} AND instrument_id = #{instrumentId}")
    Holdings selectHoldingByAccountAndInstrument(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId);

    @Select("SELECT holding_id as holdingId, account_id as accountId, instrument_id as instrumentId, quantity, updated_at as updatedAt FROM trading.holdings WHERE account_id = #{accountId} LIMIT #{limit} OFFSET #{offset}")
    List<Holdings> selectHoldingsByAccountId(@Param("accountId") UUID accountId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.holdings WHERE account_id = #{accountId}")
    int countHoldingsByAccountId(@Param("accountId") UUID accountId);

    @Select("SELECT holding_id as holdingId, account_id as accountId, instrument_id as instrumentId, quantity, updated_at as updatedAt FROM trading.holdings WHERE instrument_id = #{instrumentId} LIMIT #{limit} OFFSET #{offset}")
    List<Holdings> selectHoldingsByInstrumentId(@Param("instrumentId") UUID instrumentId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.holdings WHERE instrument_id = #{instrumentId}")
    int countHoldingsByInstrumentId(@Param("instrumentId") UUID instrumentId);

    @Insert("INSERT INTO trading.holdings (account_id, instrument_id, quantity, updated_at) VALUES (#{accountId}, #{instrumentId}, #{quantity}, CURRENT_TIMESTAMP)")
    void insertHolding(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId, @Param("quantity") BigDecimal quantity);

    @Update("UPDATE trading.holdings SET quantity = #{quantity}, updated_at = CURRENT_TIMESTAMP WHERE account_id = #{accountId} AND instrument_id = #{instrumentId}")
    void updateHolding(@Param("accountId") UUID accountId, @Param("instrumentId") UUID instrumentId, @Param("quantity") BigDecimal quantity);

    @Delete("DELETE FROM trading.holdings WHERE holding_id = #{holdingId}")
    void deleteHolding(@Param("holdingId") UUID holdingId);
}
