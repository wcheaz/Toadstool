package com.neueda.leap.mapper;

import com.neueda.leap.dto.AdminDtos.AccessEvent;
import com.neueda.leap.dto.AdminDtos.ClientAccount;
import com.neueda.leap.dto.AdminDtos.ClientSummary;
import com.neueda.leap.dto.AdminDtos.InstrumentActivityRow;
import com.neueda.leap.dto.AdminDtos.LoginEventRow;
import com.neueda.leap.dto.AdminDtos.PlatformActivityRow;
import com.neueda.leap.dto.AdminDtos.PlatformOverview;
import com.neueda.leap.dto.AdminDtos.TopInstrument;
import com.neueda.leap.dto.AdminDtos.TradeEventRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-side queries for the admin oversight dashboard, audit log and analytics.
 */
@Mapper
public interface AdminQueryMapper {

    String CLIENT_SUMMARY_COLUMNS =
            "SELECT c.client_id, c.display_name, c.email, cc.username, c.status, c.created_at, "
            + "COALESCE(a.account_count, 0) AS account_count, COALESCE(a.total_cash, 0) AS total_cash, "
            + "pa.account_id AS primary_account_id, pa.account_name AS primary_account_name, "
            + "la.activity AS latest_activity, la.occurred_at AS latest_activity_at, "
            + "cc.last_login_at "
            + "FROM trading.clients c "
            + "LEFT JOIN LATERAL (SELECT username, last_login_at FROM trading.client_credentials "
            + "  WHERE client_id = c.client_id ORDER BY created_at LIMIT 1) cc ON true "
            + "LEFT JOIN LATERAL (SELECT COUNT(*) AS account_count, SUM(balance) AS total_cash "
            + "  FROM trading.accounts WHERE client_id = c.client_id) a ON true "
            + "LEFT JOIN LATERAL (SELECT account_id, account_name FROM trading.accounts "
            + "  WHERE client_id = c.client_id ORDER BY opened_at LIMIT 1) pa ON true "
            + "LEFT JOIN LATERAL (SELECT activity, occurred_at FROM ("
            + "    SELECT te.action AS activity, te.occurred_at FROM trading.trade_events te WHERE te.client_id = c.client_id "
            + "    UNION ALL "
            + "    SELECT ce.event_type AS activity, ce.occurred_at FROM trading.cash_events ce "
            + "      JOIN trading.accounts ca ON ca.account_id = ce.account_id WHERE ca.client_id = c.client_id"
            + "  ) activity ORDER BY occurred_at DESC LIMIT 1) la ON true ";

    String CLIENT_SEARCH_FILTER =
            "<where>"
            + "<if test='q != null'>(c.display_name ILIKE '%' || #{q} || '%' OR c.email ILIKE '%' || #{q} || '%' "
            + "OR cc.username ILIKE '%' || #{q} || '%' OR c.client_id::text ILIKE #{q} || '%')</if>"
            + "<if test='status != null'> AND c.status = #{status}</if>"
            + "</where>";

    @Select({"<script>", CLIENT_SUMMARY_COLUMNS, CLIENT_SEARCH_FILTER,
            "ORDER BY c.display_name, c.client_id LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<ClientSummary> searchClients(@Param("q") String q, @Param("status") String status,
                                      @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM trading.clients c",
            "LEFT JOIN LATERAL (SELECT username FROM trading.client_credentials WHERE client_id = c.client_id ORDER BY created_at LIMIT 1) cc ON true",
            CLIENT_SEARCH_FILTER, "</script>"})
    int countClients(@Param("q") String q, @Param("status") String status);

    @Select({CLIENT_SUMMARY_COLUMNS, "WHERE c.client_id = #{clientId}"})
    ClientSummary selectClientSummary(@Param("clientId") UUID clientId);

    @Select("SELECT account_id, account_name, status, opened_at, balance FROM trading.accounts "
            + "WHERE client_id = #{clientId} ORDER BY opened_at")
    List<ClientAccount> selectClientAccounts(@Param("clientId") UUID clientId);

    // ---- Audit: admin access ----

    @Insert("INSERT INTO trading.admin_access_events (admin_user_id, admin_email, client_id, action, details) "
            + "VALUES (#{adminUserId}, #{adminEmail}, #{clientId}, #{action}, CAST(#{details} AS jsonb))")
    void insertAccessEvent(@Param("adminUserId") UUID adminUserId, @Param("adminEmail") String adminEmail,
                           @Param("clientId") UUID clientId, @Param("action") String action,
                           @Param("details") String details);

    String ACCESS_FILTER = "<where><if test='clientId != null'>e.client_id = #{clientId}</if></where>";

    @Select({"<script>",
            "SELECT e.access_event_id, e.admin_user_id, e.admin_email, e.client_id, c.display_name AS client_name,",
            "e.action, e.occurred_at, e.details::text AS details",
            "FROM trading.admin_access_events e LEFT JOIN trading.clients c ON c.client_id = e.client_id",
            ACCESS_FILTER, "ORDER BY e.occurred_at DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<AccessEvent> selectAccessEvents(@Param("clientId") UUID clientId,
                                         @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM trading.admin_access_events e", ACCESS_FILTER, "</script>"})
    int countAccessEvents(@Param("clientId") UUID clientId);

    // ---- Audit: trade events across the platform ----

    String TRADE_FILTER = "<where>"
            + "<if test='clientId != null'>e.client_id = #{clientId}</if>"
            + "<if test='action != null'> AND e.action = #{action}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT e.trade_event_id, e.client_id, c.display_name AS client_name, e.entity_type, e.entity_id,",
            "e.action, e.occurred_at, e.details::text AS details",
            "FROM trading.trade_events e LEFT JOIN trading.clients c ON c.client_id = e.client_id",
            TRADE_FILTER, "ORDER BY e.occurred_at DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<TradeEventRow> selectTradeEvents(@Param("clientId") UUID clientId, @Param("action") String action,
                                          @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM trading.trade_events e", TRADE_FILTER, "</script>"})
    int countTradeEvents(@Param("clientId") UUID clientId, @Param("action") String action);

    // ---- Audit: login events with filters ----

    String LOGIN_FILTER = "<where>"
            + "<if test='clientId != null'>e.client_id = #{clientId}</if>"
            + "<if test='outcome != null'> AND e.outcome = #{outcome}</if>"
            + "<if test='emailAttempted != null'> AND e.email_attempted ILIKE '%' || #{emailAttempted} || '%'</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT e.login_event_id, e.client_id, c.display_name AS client_name, e.email_attempted, e.outcome,",
            "e.occurred_at, e.details::text AS details",
            "FROM trading.login_events e LEFT JOIN trading.clients c ON c.client_id = e.client_id",
            LOGIN_FILTER, "ORDER BY e.occurred_at DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<LoginEventRow> selectLoginEvents(@Param("clientId") UUID clientId, @Param("outcome") String outcome,
                                          @Param("emailAttempted") String emailAttempted,
                                          @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM trading.login_events e", LOGIN_FILTER, "</script>"})
    int countLoginEvents(@Param("clientId") UUID clientId, @Param("outcome") String outcome,
                         @Param("emailAttempted") String emailAttempted);

    // ---- Analytics ----

    String DATE_FILTER = "<where>"
            + "<if test='fromDate != null'>activity_date &gt;= #{fromDate}</if>"
            + "<if test='toDate != null'> AND activity_date &lt;= #{toDate}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT activity_date, order_count, filled_quantity, gross_amount, updated_at",
            "FROM analytics.fact_daily_platform_activity", DATE_FILTER,
            "ORDER BY activity_date DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<PlatformActivityRow> selectPlatformActivity(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                                     @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.fact_daily_platform_activity", DATE_FILTER, "</script>"})
    int countPlatformActivity(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    String INSTRUMENT_DATE_FILTER = "<where>"
            + "<if test='instrumentId != null'>f.instrument_id = #{instrumentId}</if>"
            + "<if test='fromDate != null'> AND f.activity_date &gt;= #{fromDate}</if>"
            + "<if test='toDate != null'> AND f.activity_date &lt;= #{toDate}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT f.activity_date, f.instrument_id, i.symbol, f.order_count, f.filled_quantity, f.gross_amount, f.updated_at",
            "FROM analytics.fact_daily_instrument_activity f JOIN trading.instruments i ON i.instrument_id = f.instrument_id",
            INSTRUMENT_DATE_FILTER,
            "ORDER BY f.activity_date DESC, f.gross_amount DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<InstrumentActivityRow> selectInstrumentActivity(@Param("instrumentId") UUID instrumentId,
                                                         @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                                         @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.fact_daily_instrument_activity f", INSTRUMENT_DATE_FILTER, "</script>"})
    int countInstrumentActivity(@Param("instrumentId") UUID instrumentId,
                                @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Select("SELECT "
            + "(SELECT COUNT(*) FROM trading.clients) AS total_clients, "
            + "(SELECT COUNT(*) FROM trading.clients WHERE status = 'ACTIVE') AS active_clients, "
            + "(SELECT COUNT(*) FROM trading.accounts) AS total_accounts, "
            + "(SELECT COALESCE(SUM(balance), 0) FROM trading.accounts) AS total_cash, "
            + "(SELECT COUNT(*) FROM trading.orders) AS total_orders, "
            + "(SELECT COUNT(DISTINCT order_id) FROM trading.fills WHERE status = 'Filled') AS filled_orders, "
            + "(SELECT COUNT(*) FROM trading.orders o WHERE NOT EXISTS "
            + "  (SELECT 1 FROM trading.fills f WHERE f.order_id = o.order_id)) AS open_orders, "
            + "(SELECT COUNT(*) FROM trading.orders WHERE submitted_at >= now() - interval '30 days') AS orders_last30_days, "
            + "(SELECT COALESCE(SUM(price * quantity), 0) FROM trading.fills "
            + "  WHERE status = 'Filled' AND executed_at >= now() - interval '30 days') AS gross_volume_last30_days, "
            + "(SELECT COALESCE(SUM(fee), 0) FROM trading.fills "
            + "  WHERE status = 'Filled' AND executed_at >= now() - interval '30 days') AS fees_last30_days, "
            + "(SELECT COUNT(*) FROM trading.login_events "
            + "  WHERE outcome = 'FAILURE' AND occurred_at >= now() - interval '24 hours') AS failed_logins_last24_hours")
    PlatformOverview selectPlatformOverview();

    @Select("SELECT i.instrument_id, i.symbol, i.name, COUNT(*) AS fill_count, SUM(f.price * f.quantity) AS gross_amount "
            + "FROM trading.fills f "
            + "JOIN trading.orders o ON o.order_id = f.order_id "
            + "JOIN trading.instruments i ON i.instrument_id = o.instrument_id "
            + "WHERE f.status = 'Filled' AND f.executed_at >= now() - interval '30 days' "
            + "GROUP BY i.instrument_id, i.symbol, i.name "
            + "ORDER BY gross_amount DESC LIMIT #{limit}")
    List<TopInstrument> selectTopInstruments(@Param("limit") int limit);
}
