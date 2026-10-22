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
            + "'Client ' || SUBSTRING(a.client_id::text, 1, 8) AS display_name, "
            + "COALESCE(ll.email_attempted, '') AS email, "
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
            "CASE WHEN e.client_id IS NULL THEN NULL::varchar ELSE 'Client ' || SUBSTRING(e.client_id::text, 1, 8) END AS client_name,",
            "e.action, e.occurred_at, e.details::text AS details",
            "FROM analytics.admin_access_events e",
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
            "'Client ' || SUBSTRING(o.client_id::text, 1, 8) AS client_name,",
            "o.account_id, a.account_name, 'ORDER' AS entity_type, o.order_id AS entity_id,",
            "'ORDER_SUBMITTED' AS action, o.submitted_at AS occurred_at,",
            "json_build_object('side', o.side, 'quantity', o.quantity, 'instrumentId', o.instrument_id)::text AS details",
            "FROM analytics.orders o",
            "LEFT JOIN analytics.accounts a ON a.account_id = o.account_id",
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
            "SELECT e.login_event_id, e.client_id, 'Client ' || SUBSTRING(e.client_id::text, 1, 8) AS client_name, e.email_attempted, e.outcome,",
            "e.occurred_at, e.details::text AS details",
            "FROM analytics.login_events e",
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
            "SELECT e.cash_event_id, e.account_id, a.client_id, 'Client ' || SUBSTRING(a.client_id::text, 1, 8) AS client_name,",
            "a.account_name, e.event_type, e.amount, e.occurred_at, e.details::text AS details",
            "FROM analytics.cash_movements e",
            "JOIN analytics.accounts a ON a.account_id = e.account_id",
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
            + "(SELECT COUNT(DISTINCT client_id) FROM analytics.accounts) AS total_clients, "
            + "(SELECT COUNT(DISTINCT client_id) FROM analytics.accounts WHERE status = 'ACTIVE') AS active_clients, "
            + "(SELECT COUNT(*) FROM analytics.accounts) AS total_accounts, "
            + "(SELECT COALESCE(SUM(balance), 0) FROM analytics.accounts) AS total_cash, "
            + "(SELECT COUNT(*) FROM analytics.orders) AS total_orders, "
            + "(SELECT COUNT(DISTINCT order_id) FROM analytics.cash_movements "
            + "  WHERE order_id IS NOT NULL AND event_type IN ('BUY', 'SELL')) AS filled_orders, "
            + "(SELECT COUNT(*) FROM analytics.orders o WHERE NOT EXISTS "
            + "  (SELECT 1 FROM analytics.cash_movements cm WHERE cm.order_id = o.order_id AND cm.event_type IN ('BUY', 'SELL'))) AS open_orders, "
            + "(SELECT COUNT(*) FROM analytics.orders WHERE submitted_at >= now() - interval '30 days') AS orders_last30_days, "
            + "(SELECT COALESCE(SUM(ABS(signed_amount)), 0) FROM analytics.cash_movements "
            + "  WHERE event_type IN ('BUY', 'SELL') AND occurred_at >= now() - interval '30 days') AS gross_volume_last30_days, "
            + "CAST(0 AS numeric) AS fees_last30_days, "
            + "(SELECT COUNT(*) FROM analytics.login_events "
            + "  WHERE outcome = 'FAILURE' AND occurred_at >= now() - interval '24 hours') AS failed_logins_last24_hours")
    PlatformOverview selectPlatformOverview();

    @Select("SELECT cm.instrument_id, "
            + "COALESCE(MAX(cm.instrument_symbol), MAX(i.symbol), 'N/A') AS symbol, "
            + "COALESCE(MAX(cm.instrument_name), MAX(i.name), 'Unknown') AS name, "
            + "COUNT(*) AS fill_count, COALESCE(SUM(ABS(cm.signed_amount)), 0) AS gross_amount "
            + "FROM analytics.cash_movements cm "
            + "LEFT JOIN analytics.instruments i ON i.instrument_id = cm.instrument_id "
            + "WHERE cm.instrument_id IS NOT NULL AND cm.event_type IN ('BUY', 'SELL') "
            + "AND cm.occurred_at >= now() - interval '30 days' "
            + "GROUP BY cm.instrument_id "
            + "ORDER BY gross_amount DESC LIMIT #{limit}")
    List<TopInstrument> selectTopInstruments(@Param("limit") int limit);
}
