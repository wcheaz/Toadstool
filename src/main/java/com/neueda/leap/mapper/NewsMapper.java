package com.neueda.leap.mapper;

import com.neueda.leap.News;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface NewsMapper {

    @Insert("INSERT INTO trading.news (symbol, category, headline, summary, source, published_timestamp, image_url, source_url, batch_id) " +
            "VALUES (#{symbol}, #{category}, #{headline}, #{summary}, #{source}, #{publishedTimestamp}, #{imageUrl}, #{sourceUrl}, #{batchId})")
    void insertNews(News news);

    @Delete("DELETE FROM trading.news WHERE batch_id = #{batchId}")
    void deleteNewsByBatchId(String batchId);

    @Select("SELECT news_id as newsId, symbol, category, headline, summary, source, published_timestamp as publishedTimestamp, " +
            "image_url as imageUrl, source_url as sourceUrl, created_at as createdAt, batch_id as batchId " +
            "FROM trading.news WHERE symbol = #{symbol} ORDER BY published_timestamp DESC LIMIT #{limit}")
    List<News> selectLatestNewsBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Select("SELECT news_id as newsId, symbol, category, headline, summary, source, published_timestamp as publishedTimestamp, " +
            "image_url as imageUrl, source_url as sourceUrl, created_at as createdAt, batch_id as batchId " +
            "FROM trading.news WHERE category = 'market' ORDER BY published_timestamp DESC LIMIT #{limit}")
    List<News> selectLatestGeneralNews(@Param("limit") int limit);

    @Select("<script>" +
            "SELECT news_id as newsId, symbol, category, headline, summary, source, published_timestamp as publishedTimestamp, " +
            "image_url as imageUrl, source_url as sourceUrl, created_at as createdAt, batch_id as batchId " +
            "FROM trading.news WHERE symbol IN " +
            "<foreach item='symbol' collection='symbols' open='(' separator=',' close=')'>" +
            "#{symbol}" +
            "</foreach> " +
            "ORDER BY published_timestamp DESC LIMIT #{limit}" +
            "</script>")
    List<News> selectLatestNewsBySymbols(@Param("symbols") List<String> symbols, @Param("limit") int limit);

    @Select("SELECT news_id as newsId, symbol, category, headline, summary, source, published_timestamp as publishedTimestamp, " +
            "image_url as imageUrl, source_url as sourceUrl, created_at as createdAt, batch_id as batchId " +
            "FROM trading.news WHERE batch_id = #{batchId} ORDER BY published_timestamp DESC")
    List<News> selectNewsByBatchId(@Param("batchId") String batchId);
}
