package com.neueda.leap.auth;

import org.springframework.http.HttpStatus;

public class AuthApiException extends RuntimeException {

    private final HttpStatus status;

    public AuthApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
