package com.neueda.leap.mapper;

import com.neueda.leap.LoginEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis mapper for LoginEvent (security audit)
 */
@Mapper
public interface LoginEventMapper {

    @Select("SELECT login_event_id as loginEventId, client_id as clientId, " +
            "email_attempted as emailAttempted, outcome, occurred_at as occurredAt, details " +
            "FROM trading.login_events WHERE login_event_id = #{loginEventId}")
    LoginEvent selectLoginEventById(@Param("loginEventId") UUID loginEventId);

    @Select("SELECT login_event_id as loginEventId, client_id as clientId, " +
            "email_attempted as emailAttempted, outcome, occurred_at as occurredAt, details " +
            "FROM trading.login_events WHERE client_id = #{clientId} " +
            "ORDER BY occurred_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<LoginEvent> selectLoginEventsByClientId(@Param("clientId") UUID clientId, 
                                                   @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.login_events WHERE client_id = #{clientId}")
    int countLoginEventsByClientId(@Param("clientId") UUID clientId);

    @Select("SELECT login_event_id as loginEventId, client_id as clientId, " +
            "email_attempted as emailAttempted, outcome, occurred_at as occurredAt, details " +
            "FROM trading.login_events " +
            "ORDER BY occurred_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<LoginEvent> selectAllLoginEvents(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM trading.login_events")
    int countAllLoginEvents();

    @Insert("INSERT INTO trading.login_events (client_id, email_attempted, outcome, details) " +
            "VALUES (#{clientId}, #{emailAttempted}, #{outcome}, #{details}::jsonb)")
    void insertLoginEvent(@Param("clientId") UUID clientId, @Param("emailAttempted") String emailAttempted,
                          @Param("outcome") String outcome, @Param("details") String details);
}
