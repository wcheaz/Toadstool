package com.neueda.leap.auth;

import com.neueda.leap.Account;
import com.neueda.leap.Client;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.mapper.AccountMapper;
import com.neueda.leap.mapper.ClientMapper;
import com.neueda.leap.security.JwtTokenService;
import com.neueda.leap.security.JwtTokenService.IssuedTokenPair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AuthService {

    private final ClientMapper clientMapper;
    private final AccountMapper accountMapper;
    private final AuthCredentialMapper authCredentialMapper;
    private final SessionKeyService sessionKeyService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(ClientMapper clientMapper, AccountMapper accountMapper, AuthCredentialMapper authCredentialMapper,
                       SessionKeyService sessionKeyService, JwtTokenService jwtTokenService, PasswordEncoder passwordEncoder) {
        this.clientMapper = clientMapper;
        this.accountMapper = accountMapper;
        this.authCredentialMapper = authCredentialMapper;
        this.sessionKeyService = sessionKeyService;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);
        if (clientMapper.selectClientByEmail(request.getEmail()) != null) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (authCredentialMapper.selectByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("Username is already registered");
        }

        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID credentialId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        clientMapper.insertClient(clientId, request.getEmail(), request.getDisplayName(), ClientStatus.ACTIVE.name());
        accountMapper.insertAccountWithId(accountId, clientId, AccountStatus.ACTIVE.name());
        authCredentialMapper.insertCredential(credentialId, accountId, request.getUsername(), encodedPassword);

        Client client = new Client();
        client.setClientId(clientId);
        client.setEmail(request.getEmail());
        client.setDisplayName(request.getDisplayName());
        client.setStatus(ClientStatus.ACTIVE);

        Account account = new Account();
        account.setAccountId(accountId);
        account.setClientId(clientId);
        account.setStatus(AccountStatus.ACTIVE);
        return issueAuthResponse(client, account, request.getUsername(), credentialId);
    }

    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);
        AuthCredential credential = authCredentialMapper.selectByUsername(request.getUsername());
        if (credential == null || !passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        authCredentialMapper.markLastLogin(credential.getCredentialId());
        Account account = accountMapper.selectAccountById(credential.getAccountId());
        Client client = clientMapper.selectClientById(account.getClientId());
        return issueAuthResponse(client, account, credential.getUsername(), credential.getCredentialId());
    }

    private AuthResponse issueAuthResponse(Client client, Account account, String username, UUID credentialId) {
        if (client == null || account == null) {
            throw new IllegalStateException("Client and account are required to issue JWTs");
        }
        if (client.getClientId() == null) {
            throw new IllegalStateException("Client ID is required to issue JWTs");
        }
        if (account.getAccountId() == null) {
            throw new IllegalStateException("Account ID is required to issue JWTs");
        }
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw new IllegalStateException("Client email is required to issue JWTs");
        }

        UUID sessionKeyId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        IssuedTokenPair tokenPair = jwtTokenService.issueTokenPair(
                sessionKeyId,
                client.getClientId(),
                account.getAccountId(),
                username,
                client.getEmail(),
                "CLIENT"
        );

        if (tokenPair.getAccessToken() == null) {
            throw new IllegalStateException("JWT authentication is disabled");
        }

        sessionKeyService.createSession(
                sessionKeyId,
                credentialId,
                account.getAccountId(),
                tokenPair.getRefreshToken(),
                issuedAt,
                OffsetDateTime.ofInstant(tokenPair.getAccessTokenExpiresAt(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(tokenPair.getRefreshTokenExpiresAt(), ZoneOffset.UTC)
        );

        return new AuthResponse(
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                Duration.between(issuedAt.toInstant(), tokenPair.getAccessTokenExpiresAt()).getSeconds(),
                client.getClientId(),
                account.getAccountId(),
                username,
                client.getEmail(),
                "CLIENT"
        );
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
                || request.getDisplayName() == null || request.getDisplayName().isBlank()
                || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Email, display name, username, and password are required");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        validateRefreshRequest(request);
        AuthSession session = sessionKeyService.findActiveSessionByRefreshToken(request.getRefreshToken());
        if (session == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        AuthCredential credential = authCredentialMapper.selectByAccountId(session.getAccountId());
        if (credential == null) {
            throw new IllegalStateException("Credential not found for session");
        }

        Account account = accountMapper.selectAccountById(session.getAccountId());
        Client client = clientMapper.selectClientById(account.getClientId());
        if (client == null || account == null) {
            throw new IllegalStateException("Client and account are required to refresh tokens");
        }

        sessionKeyService.touchSession(session.getSessionKeyId());
        return issueAuthResponse(client, account, credential.getUsername(), credential.getCredentialId());
    }

    private void validateRefreshRequest(RefreshRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
    }

}
