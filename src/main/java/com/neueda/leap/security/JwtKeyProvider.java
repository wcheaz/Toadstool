package com.neueda.leap.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class JwtKeyProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyProvider.class);
    private static final String PLACEHOLDER_PUBLIC_KEY = "BASE64_X509_PUBLIC_KEY";
    private static final String PLACEHOLDER_PRIVATE_KEY = "BASE64_PKCS8_PRIVATE_KEY";

    private final JwtProperties jwtProperties;

    private PublicKey verificationKey;
    private PrivateKey signingKey;

    public JwtKeyProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void initialize() {
        if (!jwtProperties.isEnabled()) {
            return;
        }

        boolean hasConfiguredPublicKey = hasConfiguredKey(jwtProperties.getPublicKey(), PLACEHOLDER_PUBLIC_KEY);
        boolean hasConfiguredPrivateKey = hasConfiguredKey(jwtProperties.getPrivateKey(), PLACEHOLDER_PRIVATE_KEY);

        if (hasConfiguredPublicKey != hasConfiguredPrivateKey) {
            throw new IllegalStateException("auth.jwt.public-key and auth.jwt.private-key must both be configured");
        }

        if (!hasConfiguredPublicKey) {
            KeyPair generatedKeyPair = generateKeyPair();
            verificationKey = generatedKeyPair.getPublic();
            signingKey = generatedKeyPair.getPrivate();
            logger.warn("JWT keys are not configured; generated an ephemeral RSA key pair for local development");
            return;
        }

        verificationKey = JwtKeySupport.loadPublicKey(jwtProperties.getPublicKey());
        signingKey = JwtKeySupport.loadPrivateKey(jwtProperties.getPrivateKey());
    }

    public PublicKey getVerificationKey() {
        return verificationKey;
    }

    public PrivateKey getSigningKey() {
        return signingKey;
    }

    private boolean hasConfiguredKey(String key, String placeholder) {
        return StringUtils.hasText(key) && !placeholder.equals(key.trim());
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate fallback JWT key pair", exception);
        }
    }
}
