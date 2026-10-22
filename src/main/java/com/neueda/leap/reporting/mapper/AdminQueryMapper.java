package com.neueda.leap.reporting.mapper;

import com.neueda.leap.dto.AdminDtos.AccessEvent;
import com.neueda.leap.dto.AdminDtos.AdminOrderRow;
import com.neueda.leap.dto.AdminDtos.ClientAccount;
import com.neueda.leap.dto.AdminDtos.ClientSummary;
import com.neueda.leap.dto.AdminDtos.CashEventRow;
import com.neueda.leap.dto.AdminDtos.InstrumentActivityRow;
import com.neueda.leap.dto.AdminDtos.LoginEventRow;
import com.neueda.leap.dto.AdminDtos.PlatformActivityRow;
import com.neueda.leap.dto.AdminDtos.PlatformOverview;
import com.neueda.leap.dto.AdminDtos.TopInstrument;
import com.neueda.leap.dto.AdminDtos.InstrumentSideRatio;
import com.neueda.leap.dto.AdminDtos.TradeEventRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-side queries for the admin oversight dashboard, audit log and analytics.
 */
@Mapper
public interface AdminQueryMapper {

    String CLIENT_BASE =
            "SELECT "
            + "a.client_id, "
            + "COALESCE(MAX(NULLIF(a.client_email, '')), ll.email_attempted, '') AS display_name, "
            + "COALESCE(MAX(NULLIF(a.client_email, '')), ll.email_attempted, '') AS email, "
            + "NULL::varchar AS username, "
            + "CASE WHEN BOOL_OR(a.status = 'ACTIVE') THEN 'ACTIVE' ELSE 'INACTIVE' END AS status, "
            + "MIN(a.opened_at) AS created_at, "
            + "COUNT(*) AS account_count, "
            + "COALESCE(SUM(a.balance), 0) AS total_cash, "
            + "(ARRAY_AGG(a.account_id ORDER BY a.opened_at, a.account_id))[1] AS primary_account_id, "
            + "(ARRAY_AGG(COALESCE(a.account_name, 'Account') ORDER BY a.opened_at, a.account_id))[1] AS primary_account_name, "
            + "la.activity AS latest_activity, "
            + "la.occurred_at AS latest_activity_at, "
            + "ll.last_login_at "
            + "FROM analytics.accounts a "
            + "LEFT JOIN LATERAL ("
            + "    SELECT le.email_attempted, le.occurred_at AS last_login_at "
            + "    FROM analytics.login_events le "
            + "    WHERE le.client_id = a.client_id AND le.outcome = 'SUCCESS' "
            + "    ORDER BY le.occurred_at DESC LIMIT 1"
            + ") ll ON true "
            + "LEFT JOIN LATERAL ("
            + "    SELECT activity, occurred_at FROM ("
            + "        SELECT 'ORDER_SUBMITTED' AS activity, o.submitted_at AS occurred_at "
            + "        FROM analytics.orders o "
            + "        WHERE o.client_id = a.client_id "
            + "        UNION ALL "
            + "        SELECT cm.event_type AS activity, cm.occurred_at "
            + "        FROM analytics.cash_movements cm "
            + "        WHERE cm.client_id = a.client_id"
            + "    ) events ORDER BY occurred_at DESC LIMIT 1"
            + ") la ON true ";

    String CLIENT_SEARCH_FILTER =
            "<where>"
            + "<if test='q != null'>(base.display_name ILIKE '%' || #{q} || '%' OR base.email ILIKE '%' || #{q} || '%' "
            + "OR base.client_id::text ILIKE #{q} || '%' OR base.primary_account_name ILIKE '%' || #{q} || '%')</if>"
            + "<if test='status != null'> AND base.status = #{status}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT base.client_id, base.display_name, base.email, base.username, base.status, base.created_at,",
            "base.account_count, base.total_cash, base.primary_account_id, base.primary_account_name,",
            "base.latest_activity, base.latest_activity_at, base.last_login_at",
            "FROM (", CLIENT_BASE, "GROUP BY a.client_id, ll.email_attempted, ll.last_login_at, la.activity, la.occurred_at", ") base",
            CLIENT_SEARCH_FILTER,
            "ORDER BY base.display_name, base.client_id LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<ClientSummary> searchClients(@Param("q") String q, @Param("status") String status,
                                      @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>",
            "SELECT COUNT(*) FROM (",
            "SELECT base.client_id FROM (", CLIENT_BASE, "GROUP BY a.client_id, ll.email_attempted, ll.last_login_at, la.activity, la.occurred_at", ") base",
            CLIENT_SEARCH_FILTER,
            ") counted",
            "</script>"})
    int countClients(@Param("q") String q, @Param("status") String status);

    @Select({"<script>",
            "SELECT base.client_id, base.display_name, base.email, base.username, base.status, base.created_at,",
            "base.account_count, base.total_cash, base.primary_account_id, base.primary_account_name,",
            "base.latest_activity, base.latest_activity_at, base.last_login_at",
            "FROM (", CLIENT_BASE, "GROUP BY a.client_id, ll.email_attempted, ll.last_login_at, la.activity, la.occurred_at", ") base",
            "WHERE base.client_id = #{clientId}",
            "</script>"})
    ClientSummary selectClientSummary(@Param("clientId") UUID clientId);

    @Select("SELECT account_id, account_name, status, opened_at, balance FROM analytics.accounts "
            + "WHERE client_id = #{clientId} ORDER BY opened_at")
    List<ClientAccount> selectClientAccounts(@Param("clientId") UUID clientId);

    String ORDER_FILTER = "<where>"
            + "<if test='fromSubmittedAt != null'>submitted_at &gt;= #{fromSubmittedAt}</if>"
            + "<if test='toSubmittedAt != null'> AND submitted_at &lt; #{toSubmittedAt}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT order_id, account_id, instrument_id, side, quantity, idempotency_key,",
            "CASE WHEN EXISTS (",
            "  SELECT 1 FROM analytics.cash_movements cm",
            "  WHERE cm.order_id = analytics.orders.order_id AND cm.event_type IN ('BUY', 'SELL')",
            ") THEN 'FILLED' ELSE 'SUBMITTED' END AS status,",
            "submitted_at",
            "FROM analytics.orders",
            ORDER_FILTER,
            "ORDER BY submitted_at DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<AdminOrderRow> selectAllOrders(@Param("fromSubmittedAt") OffsetDateTime fromSubmittedAt,
                                        @Param("toSubmittedAt") OffsetDateTime toSubmittedAt,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.orders", ORDER_FILTER, "</script>"})
    int countAllOrders(@Param("fromSubmittedAt") OffsetDateTime fromSubmittedAt,
                       @Param("toSubmittedAt") OffsetDateTime toSubmittedAt);

    // ---- Audit: admin access ----

    String ACCESS_FILTER = "<where><if test='clientId != null'>e.client_id = #{clientId}</if></where>";

    @Select({"<script>",
            "SELECT e.access_event_id, e.admin_user_id, e.admin_email, e.client_id,",
            "COALESCE(NULLIF(c.client_email, ''), ll.email_attempted) AS client_name,",
            "e.action, e.occurred_at, e.details::text AS details",
            "FROM analytics.admin_access_events e",
            "LEFT JOIN LATERAL (",
            "  SELECT a.client_email",
            "  FROM analytics.accounts a",
            "  WHERE a.client_id = e.client_id",
            "  ORDER BY a.opened_at, a.account_id",
            "  LIMIT 1",
            ") c ON true",
            "LEFT JOIN LATERAL (",
            "  SELECT le.email_attempted",
            "  FROM analytics.login_events le",
            "  WHERE le.client_id = e.client_id AND le.outcome = 'SUCCESS'",
            "  ORDER BY le.occurred_at DESC",
            "  LIMIT 1",
            ") ll ON true",
            ACCESS_FILTER, "ORDER BY e.occurred_at DESC, e.access_event_id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<AccessEvent> selectAccessEvents(@Param("clientId") UUID clientId,
                                         @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.admin_access_events e", ACCESS_FILTER, "</script>"})
    int countAccessEvents(@Param("clientId") UUID clientId);

    // ---- Audit: trade events across the platform ----

    String TRADE_FILTER = "<where>"
            + "<if test='clientId != null'>o.client_id = #{clientId}</if>"
            + "<if test='action != null'> AND UPPER(#{action}) = 'ORDER_SUBMITTED'</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT o.order_id AS trade_event_id, o.client_id,",
            "COALESCE(NULLIF(a.client_email, ''), ll.email_attempted) AS client_name,",
            "o.account_id, a.account_name, 'ORDER' AS entity_type, o.order_id AS entity_id,",
            "'ORDER_SUBMITTED' AS action, o.submitted_at AS occurred_at,",
            "json_build_object('side', o.side, 'quantity', o.quantity, 'instrumentId', o.instrument_id)::text AS details",
            "FROM analytics.orders o",
            "LEFT JOIN analytics.accounts a ON a.account_id = o.account_id",
            "LEFT JOIN LATERAL (",
            "  SELECT le.email_attempted",
            "  FROM analytics.login_events le",
            "  WHERE le.client_id = o.client_id AND le.outcome = 'SUCCESS'",
            "  ORDER BY le.occurred_at DESC",
            "  LIMIT 1",
            ") ll ON true",
            TRADE_FILTER, "ORDER BY o.submitted_at DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<TradeEventRow> selectTradeEvents(@Param("clientId") UUID clientId, @Param("action") String action,
                                          @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.orders e", TRADE_FILTER, "</script>"})
    int countTradeEvents(@Param("clientId") UUID clientId, @Param("action") String action);

    // ---- Audit: login events with filters ----

    String LOGIN_FILTER = "<where>"
            + "<if test='clientId != null'>e.client_id = #{clientId}</if>"
            + "<if test='outcome != null'> AND e.outcome = #{outcome}</if>"
            + "<if test='emailAttempted != null'> AND e.email_attempted ILIKE '%' || #{emailAttempted} || '%'</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT e.login_event_id, e.client_id, COALESCE(NULLIF(c.client_email, ''), e.email_attempted) AS client_name, e.email_attempted, e.outcome,",
            "e.occurred_at, e.details::text AS details",
            "FROM analytics.login_events e",
            "LEFT JOIN LATERAL (",
            "  SELECT a.client_email",
            "  FROM analytics.accounts a",
            "  WHERE a.client_id = e.client_id",
            "  ORDER BY a.opened_at, a.account_id",
            "  LIMIT 1",
            ") c ON true",
            LOGIN_FILTER, "ORDER BY e.occurred_at DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<LoginEventRow> selectLoginEvents(@Param("clientId") UUID clientId, @Param("outcome") String outcome,
                                          @Param("emailAttempted") String emailAttempted,
                                          @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.login_events e", LOGIN_FILTER, "</script>"})
    int countLoginEvents(@Param("clientId") UUID clientId, @Param("outcome") String outcome,
                         @Param("emailAttempted") String emailAttempted);

    // ---- Audit: cash events across the platform ----

    String CASH_EVENT_FILTER = "<where>e.event_type IN ('DEPOSIT', 'WITHDRAWAL')"
            + "<if test='clientId != null'> AND a.client_id = #{clientId}</if>"
            + "<if test='eventType != null'> AND e.event_type = #{eventType}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT e.cash_event_id, e.account_id, a.client_id, COALESCE(NULLIF(a.client_email, ''), ll.email_attempted) AS client_name,",
            "a.account_name, e.event_type, e.amount, e.occurred_at, e.details::text AS details",
            "FROM analytics.cash_movements e",
            "JOIN analytics.accounts a ON a.account_id = e.account_id",
            "LEFT JOIN LATERAL (",
            "  SELECT le.email_attempted",
            "  FROM analytics.login_events le",
            "  WHERE le.client_id = a.client_id AND le.outcome = 'SUCCESS'",
            "  ORDER BY le.occurred_at DESC",
            "  LIMIT 1",
            ") ll ON true",
            CASH_EVENT_FILTER,
            "ORDER BY e.occurred_at DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<CashEventRow> selectCashEvents(@Param("clientId") UUID clientId,
                                         @Param("eventType") String eventType,
                                         @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM analytics.cash_movements e",
            "JOIN analytics.accounts a ON a.account_id = e.account_id",
            CASH_EVENT_FILTER, "</script>"})
    int countCashEvents(@Param("clientId") UUID clientId, @Param("eventType") String eventType);

    // ---- Analytics ----

    String DATE_FILTER = "<where>"
            + "<if test='fromDate != null'>o.order_date &gt;= #{fromDate}</if>"
            + "<if test='toDate != null'> AND o.order_date &lt;= #{toDate}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT order_date AS activity_date,",
            "COUNT(*)::integer AS order_count,",
            "COALESCE(SUM(quantity), 0) AS filled_quantity,",
            "COALESCE(SUM(ABS(cm.signed_amount)) FILTER (WHERE cm.event_type IN ('BUY', 'SELL')), 0) AS gross_amount,",
            "MAX(COALESCE(cm.occurred_at, o.submitted_at)) AS updated_at",
            "FROM analytics.orders o",
            "LEFT JOIN analytics.cash_movements cm ON cm.order_id = o.order_id",
            DATE_FILTER,
            "GROUP BY order_date",
            "ORDER BY activity_date DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<PlatformActivityRow> selectPlatformActivity(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                                     @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM (",
            "SELECT order_date FROM analytics.orders", DATE_FILTER, "GROUP BY order_date",
            ") counted", "</script>"})
    int countPlatformActivity(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    String INSTRUMENT_DATE_FILTER = "<where>"
            + "<if test='instrumentId != null'>o.instrument_id = #{instrumentId}</if>"
            + "<if test='fromDate != null'> AND o.order_date &gt;= #{fromDate}</if>"
            + "<if test='toDate != null'> AND o.order_date &lt;= #{toDate}</if>"
            + "</where>";

    @Select({"<script>",
            "SELECT o.order_date AS activity_date, o.instrument_id,",
            "COALESCE(MAX(o.instrument_symbol), MAX(i.symbol), 'N/A') AS symbol,",
            "COUNT(*)::integer AS order_count,",
            "COALESCE(SUM(o.quantity), 0) AS filled_quantity,",
            "COALESCE(SUM(ABS(cm.signed_amount)) FILTER (WHERE cm.event_type IN ('BUY', 'SELL')), 0) AS gross_amount,",
            "MAX(COALESCE(cm.occurred_at, o.submitted_at)) AS updated_at",
            "FROM analytics.orders o",
            "LEFT JOIN analytics.instruments i ON i.instrument_id = o.instrument_id",
            "LEFT JOIN analytics.cash_movements cm ON cm.order_id = o.order_id",
            INSTRUMENT_DATE_FILTER,
            "GROUP BY o.order_date, o.instrument_id",
            "ORDER BY activity_date DESC, gross_amount DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<InstrumentActivityRow> selectInstrumentActivity(@Param("instrumentId") UUID instrumentId,
                                                         @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                                         @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM (",
            "SELECT o.order_date, o.instrument_id FROM analytics.orders o",
            INSTRUMENT_DATE_FILTER,
            "GROUP BY o.order_date, o.instrument_id",
            ") counted", "</script>"})
    int countInstrumentActivity(@Param("instrumentId") UUID instrumentId,
                                @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Select("SELECT "
            + "(SELECT COUNT(*) FROM trading.clients) AS total_clients, "
            + "(SELECT COUNT(*) FROM trading.clients WHERE status = 'ACTIVE') AS active_clients, "
            + "(SELECT COUNT(*) FROM trading.accounts) AS total_accounts, "
            + "(SELECT COALESCE(SUM(balance), 0) FROM trading.accounts) AS total_cash, "
            + "(SELECT COUNT(*) FROM trading.orders) AS total_orders, "
            + "(SELECT COALESCE(SUM(order_count), 0) FROM analytics.fact_daily_platform_activity) AS filled_orders, "
            + "(SELECT COUNT(*) FROM trading.orders o WHERE NOT EXISTS "
            + "  (SELECT 1 FROM trading.fills f WHERE f.order_id = o.order_id)) AS open_orders, "
            + "(SELECT COUNT(*) FROM trading.orders WHERE submitted_at >= now() - (CAST(#{days} AS text) || ' days')::interval) AS orders_last30_days, "
            + "p.gross_volume AS gross_volume_last30_days, p.fees AS fees_last30_days, "
            + "p.buy_fills AS buy_fills_last30_days, p.sell_fills AS sell_fills_last30_days, "
            + "p.buy_quantity AS buy_quantity_last30_days, p.sell_quantity AS sell_quantity_last30_days, "
            + "p.buy_value AS buy_value_last30_days, p.sell_value AS sell_value_last30_days, "
            + "(SELECT COUNT(*) FROM trading.login_events "
            + "  WHERE outcome = 'FAILURE' AND occurred_at >= now() - (CAST(#{days} AS text) || ' days')::interval) AS failed_logins_last24_hours "
            + "FROM (SELECT COALESCE(SUM(gross_amount), 0) AS gross_volume, COALESCE(SUM(fee_amount), 0) AS fees, "
            + "  COALESCE(SUM(buy_count), 0) AS buy_fills, COALESCE(SUM(sell_count), 0) AS sell_fills, "
            + "  COALESCE(SUM(buy_quantity), 0) AS buy_quantity, COALESCE(SUM(sell_quantity), 0) AS sell_quantity, "
            + "  COALESCE(SUM(buy_amount), 0) AS buy_value, COALESCE(SUM(sell_amount), 0) AS sell_value "
            + "  FROM analytics.fact_daily_platform_activity WHERE activity_date > (now() AT TIME ZONE 'UTC')::date - #{days}) p")
    PlatformOverview selectPlatformOverview(@Param("days") int days);

    @Select("SELECT i.instrument_id, i.symbol, i.name, SUM(f.order_count) AS fill_count, SUM(f.filled_quantity) AS filled_quantity, "
            + "SUM(f.gross_amount) AS gross_amount "
            + "FROM analytics.fact_daily_instrument_activity f "
            + "JOIN trading.instruments i ON i.instrument_id = f.instrument_id "
            + "WHERE f.activity_date > (now() AT TIME ZONE 'UTC')::date - #{days} "
            + "GROUP BY i.instrument_id, i.symbol, i.name "
            + "ORDER BY gross_amount DESC LIMIT #{limit}")
    List<TopInstrument> selectTopInstruments(@Param("limit") int limit, @Param("days") int days);

    @Select("SELECT i.instrument_id, i.symbol, i.name, SUM(f.order_count) AS fill_count, SUM(f.filled_quantity) AS filled_quantity, "
            + "SUM(f.gross_amount) AS gross_amount "
            + "FROM analytics.fact_daily_instrument_activity f "
            + "JOIN trading.instruments i ON i.instrument_id = f.instrument_id "
            + "WHERE f.activity_date > (now() AT TIME ZONE 'UTC')::date - #{days} "
            + "GROUP BY i.instrument_id, i.symbol, i.name "
            + "ORDER BY fill_count DESC, gross_amount DESC LIMIT 1")
    TopInstrument selectMostFilledInstrument(@Param("days") int days);

    @Select("SELECT i.instrument_id, i.symbol, i.name, "
            + "SUM(CASE WHEN #{side} = 'BUY' THEN f.buy_count ELSE f.sell_count END) AS fill_count, "
            + "SUM(CASE WHEN #{side} = 'BUY' THEN f.buy_amount ELSE f.sell_amount END) AS gross_amount "
            + "FROM analytics.fact_daily_instrument_activity f "
            + "JOIN trading.instruments i ON i.instrument_id = f.instrument_id "
            + "WHERE f.activity_date > (now() AT TIME ZONE 'UTC')::date - #{days} "
            + "GROUP BY i.instrument_id, i.symbol, i.name "
            + "HAVING SUM(CASE WHEN #{side} = 'BUY' THEN f.buy_count ELSE f.sell_count END) > 0 "
            + "ORDER BY gross_amount DESC, fill_count DESC LIMIT 1")
    TopInstrument selectTopInstrumentBySide(@Param("side") String side, @Param("days") int days);

    @Select("SELECT i.instrument_id, i.symbol, i.name, "
            + "SUM(f.buy_amount) AS buy_value, SUM(f.sell_amount) AS sell_value, "
            + "SUM(f.buy_amount) / NULLIF(SUM(f.sell_amount), 0) AS ratio "
            + "FROM analytics.fact_daily_instrument_activity f "
            + "JOIN trading.instruments i ON i.instrument_id = f.instrument_id "
            + "WHERE f.activity_date > (now() AT TIME ZONE 'UTC')::date - #{days} "
            + "GROUP BY i.instrument_id, i.symbol, i.name "
            + "HAVING SUM(f.sell_amount) > 0 "
            + "ORDER BY ratio DESC LIMIT 1")
    InstrumentSideRatio selectHighestBuySellRatio(@Param("days") int days);

    @Select("SELECT i.instrument_id, i.symbol, i.name, "
            + "SUM(f.buy_amount) AS buy_value, SUM(f.sell_amount) AS sell_value, "
            + "SUM(f.sell_amount) / NULLIF(SUM(f.buy_amount), 0) AS ratio "
            + "FROM analytics.fact_daily_instrument_activity f "
            + "JOIN trading.instruments i ON i.instrument_id = f.instrument_id "
            + "WHERE f.activity_date > (now() AT TIME ZONE 'UTC')::date - #{days} "
            + "GROUP BY i.instrument_id, i.symbol, i.name "
            + "HAVING SUM(f.buy_amount) > 0 "
            + "ORDER BY ratio DESC LIMIT 1")
    InstrumentSideRatio selectHighestSellBuyRatio(@Param("days") int days);
}
