package com.neueda.leap.service;

import com.neueda.leap.dto.AdminDtos.AccessEvent;
import com.neueda.leap.dto.AdminDtos.AdminOrderRow;
import com.neueda.leap.dto.AdminDtos.ClientDetail;
import com.neueda.leap.dto.AdminDtos.ClientSummary;
import com.neueda.leap.dto.AdminDtos.CashEventRow;
import com.neueda.leap.dto.AdminDtos.InstrumentActivityRow;
import com.neueda.leap.dto.AdminDtos.LoginEventRow;
import com.neueda.leap.dto.AdminDtos.PlatformActivityRow;
import com.neueda.leap.dto.AdminDtos.PlatformOverview;
import com.neueda.leap.dto.AdminDtos.TopInstrument;
import com.neueda.leap.dto.AdminDtos.TradeEventRow;
import com.neueda.leap.mapper.AdminAuditWriteMapper;
import com.neueda.leap.reporting.mapper.AdminQueryMapper;
import com.neueda.leap.security.AuthenticatedUser;
import com.neueda.leap.security.SecurityAccess;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only oversight queries for admins, plus recording which admin viewed which client.
 */
@Service
public class AdminOversightService {

    public static final String VIEW_CLIENT = "VIEW_CLIENT";

    private final AdminQueryMapper adminQueryMapper;
    private final AdminAuditWriteMapper adminAuditWriteMapper;

    public AdminOversightService(AdminQueryMapper adminQueryMapper, AdminAuditWriteMapper adminAuditWriteMapper) {
        this.adminQueryMapper = adminQueryMapper;
        this.adminAuditWriteMapper = adminAuditWriteMapper;
    }

    public List<ClientSummary> searchClients(String q, String status, int limit, int offset) {
        return adminQueryMapper.searchClients(blankToNull(q), blankToNull(status), limit, offset);
    }

    public int countClients(String q, String status) {
        return adminQueryMapper.countClients(blankToNull(q), blankToNull(status));
    }

    /**
     * Load a client with all of their accounts and record the view in the admin access log.
     * Returns null when the client does not exist.
     */
    public ClientDetail viewClient(UUID clientId) {
        ClientSummary summary = adminQueryMapper.selectClientSummary(clientId);
        if (summary == null) {
            return null;
        }
        ClientDetail detail = new ClientDetail();
        detail.client = summary;
        detail.accounts = adminQueryMapper.selectClientAccounts(clientId);
        recordAccess(clientId, VIEW_CLIENT);
        return detail;
    }

    public void recordAccess(UUID clientId, String action) {
        AuthenticatedUser admin = SecurityAccess.currentUser();
        if (admin == null) {
            return;
        }
        UUID adminUserId = admin.getClientId();
        if (adminUserId == null && admin.getSubject() != null) {
            adminUserId = UUID.fromString(admin.getSubject());
        }
        if (adminUserId == null) {
            return;
        }
        adminAuditWriteMapper.insertAccessEvent(adminUserId,
                admin.getEmail() != null ? admin.getEmail() : admin.getUsername(),
                clientId, action, "{}");
    }

    public List<AccessEvent> listAccessEvents(UUID clientId, int limit, int offset) {
        return adminQueryMapper.selectAccessEvents(clientId, limit, offset);
    }

    public int countAccessEvents(UUID clientId) {
        return adminQueryMapper.countAccessEvents(clientId);
    }

    public List<TradeEventRow> listTradeEvents(UUID clientId, String action, int limit, int offset) {
        return adminQueryMapper.selectTradeEvents(clientId, blankToNull(action), limit, offset);
    }

    public int countTradeEvents(UUID clientId, String action) {
        return adminQueryMapper.countTradeEvents(clientId, blankToNull(action));
    }

    public List<LoginEventRow> listLoginEvents(UUID clientId, String outcome, String emailAttempted, int limit, int offset) {
        return adminQueryMapper.selectLoginEvents(clientId, blankToNull(outcome), blankToNull(emailAttempted), limit, offset);
    }

    public int countLoginEvents(UUID clientId, String outcome, String emailAttempted) {
        return adminQueryMapper.countLoginEvents(clientId, blankToNull(outcome), blankToNull(emailAttempted));
    }

    public List<CashEventRow> listCashEvents(UUID clientId, String eventType, int limit, int offset) {
        return adminQueryMapper.selectCashEvents(clientId, blankToNull(eventType), limit, offset);
    }

    public int countCashEvents(UUID clientId, String eventType) {
        return adminQueryMapper.countCashEvents(clientId, blankToNull(eventType));
    }

    public List<PlatformActivityRow> listPlatformActivity(LocalDate fromDate, LocalDate toDate, int limit, int offset) {
        return adminQueryMapper.selectPlatformActivity(fromDate, toDate, limit, offset);
    }

    public int countPlatformActivity(LocalDate fromDate, LocalDate toDate) {
        return adminQueryMapper.countPlatformActivity(fromDate, toDate);
    }

    public List<InstrumentActivityRow> listInstrumentActivity(UUID instrumentId, LocalDate fromDate, LocalDate toDate,
                                                              int limit, int offset) {
        return adminQueryMapper.selectInstrumentActivity(instrumentId, fromDate, toDate, limit, offset);
    }

    public int countInstrumentActivity(UUID instrumentId, LocalDate fromDate, LocalDate toDate) {
        return adminQueryMapper.countInstrumentActivity(instrumentId, fromDate, toDate);
    }

    public List<AdminOrderRow> listAllOrders(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt,
                                             int limit, int offset) {
        return adminQueryMapper.selectAllOrders(fromSubmittedAt, toSubmittedAt, limit, offset);
    }

    public int countAllOrders(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt) {
        return adminQueryMapper.countAllOrders(fromSubmittedAt, toSubmittedAt);
    }

    public PlatformOverview getPlatformOverview() {
        return adminQueryMapper.selectPlatformOverview();
    }

    public List<TopInstrument> listTopInstruments(int limit) {
        return adminQueryMapper.selectTopInstruments(limit);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
