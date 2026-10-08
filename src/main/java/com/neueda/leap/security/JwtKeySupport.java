package com.neueda.leap.security;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import io.jsonwebtoken.SignatureAlgorithm;

final class JwtKeySupport {

    private JwtKeySupport() {
    }

    static PublicKey loadPublicKey(String encodedKey) {
        byte[] decodedKey = decodeBase64(encodedKey, "auth.jwt.public-key");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        PublicKey rsaKey = tryGeneratePublicKey("RSA", keySpec);
        if (rsaKey != null) {
            return rsaKey;
        }
        PublicKey ecKey = tryGeneratePublicKey("EC", keySpec);
        if (ecKey != null) {
            return ecKey;
        }
        throw new IllegalStateException("auth.jwt.public-key must be a valid X.509 RSA or EC public key");
    }

    static PrivateKey loadPrivateKey(String encodedKey) {
        byte[] decodedKey = decodeBase64(encodedKey, "auth.jwt.private-key");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        PrivateKey rsaKey = tryGeneratePrivateKey("RSA", keySpec);
        if (rsaKey != null) {
            return rsaKey;
        }
        PrivateKey ecKey = tryGeneratePrivateKey("EC", keySpec);
        if (ecKey != null) {
            return ecKey;
        }
        throw new IllegalStateException("auth.jwt.private-key must be a valid PKCS#8 RSA or EC private key");
    }

    private static byte[] decodeBase64(String value, String propertyName) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(propertyName + " must be valid Base64", e);
        }
    }

    private static PublicKey tryGeneratePublicKey(String algorithm, X509EncodedKeySpec keySpec) {
        try {
            return KeyFactory.getInstance(algorithm).generatePublic(keySpec);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    private static PrivateKey tryGeneratePrivateKey(String algorithm, PKCS8EncodedKeySpec keySpec) {
        try {
            return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    static SignatureAlgorithm signatureAlgorithm(Key key) {
        String algorithm = key.getAlgorithm();
        if ("RSA".equalsIgnoreCase(algorithm)) {
            return SignatureAlgorithm.RS256;
        }
        if ("EC".equalsIgnoreCase(algorithm)) {
            return SignatureAlgorithm.ES256;
        }
        throw new IllegalStateException("Unsupported JWT key algorithm: " + algorithm);
    }
}
