package com.neueda.leap.job;

import com.neueda.leap.auth.SessionKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionCleanupJob {
    private static final Logger logger = LoggerFactory.getLogger(AuthSessionCleanupJob.class);

    private final SessionKeyService sessionKeyService;

    public AuthSessionCleanupJob(SessionKeyService sessionKeyService) {
        this.sessionKeyService = sessionKeyService;
    }

    @Scheduled(fixedDelayString = "${auth.sessions.cleanup-delay-ms:60000}")
    public void revokeExpiredSessions() {
        int revokedCount = sessionKeyService.revokeExpiredSessions();
        if (revokedCount > 0) {
            logger.info("Revoked {} expired auth sessions", revokedCount);
        }
    }
}
