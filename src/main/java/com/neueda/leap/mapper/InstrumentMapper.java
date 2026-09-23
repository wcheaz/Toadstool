package com.neueda.leap.mapper;

import com.neueda.leap.Instrument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.UUID;

@Mapper
public interface InstrumentMapper {

    @Select("SELECT instrument_id as instrumentId, symbol, name, asset_class as assetClass, status FROM trading.instruments WHERE instrument_id = #{instrumentId}")
    Instrument selectInstrumentById(@Param("instrumentId") UUID instrumentId);

    @Select("SELECT instrument_id as instrumentId, symbol, name, asset_class as assetClass, status FROM trading.instruments WHERE symbol = #{symbol}")
    Instrument selectInstrumentBySymbol(@Param("symbol") String symbol);

    @Select("SELECT instrument_id as instrumentId, symbol, name, asset_class as assetClass, status FROM trading.instruments WHERE status = 'TRADABLE' ORDER BY symbol LIMIT #{limit} OFFSET #{offset}")
    List<Instrument> selectTradableInstruments(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.instruments WHERE status = 'TRADABLE'")
    int countTradableInstruments();

    @Select("SELECT instrument_id as instrumentId, symbol, name, asset_class as assetClass, status FROM trading.instruments ORDER BY symbol LIMIT #{limit} OFFSET #{offset}")
    List<Instrument> selectAllInstruments(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.instruments")
    int countAllInstruments();

    @Select("SELECT instrument_id as instrumentId, symbol, name, asset_class as assetClass, status FROM trading.instruments WHERE status = #{status} LIMIT #{limit} OFFSET #{offset}")
    List<Instrument> selectInstrumentsByStatus(@Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT instrument_id as instrumentId, symbol, name, asset_class as assetClass, status FROM trading.instruments WHERE asset_class = #{assetClass} LIMIT #{limit} OFFSET #{offset}")
    List<Instrument> selectInstrumentsByAssetClass(@Param("assetClass") String assetClass, @Param("limit") int limit, @Param("offset") int offset);

    @Insert("INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES (#{symbol}, #{name}, #{assetClass}, 'TRADABLE')")
    void insertInstrument(@Param("symbol") String symbol, @Param("name") String name, @Param("assetClass") String assetClass);

    @Update("UPDATE trading.instruments SET status = #{status} WHERE instrument_id = #{instrumentId}")
    void updateInstrumentStatus(@Param("instrumentId") UUID instrumentId, @Param("status") String status);
}
