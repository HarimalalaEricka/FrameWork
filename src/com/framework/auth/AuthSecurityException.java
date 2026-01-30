package com.framework.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Exception de sécurité
 */
public class AuthSecurityException extends RuntimeException {
    private int statusCode;
    
    public AuthSecurityException(String message) {
        super(message);
        this.statusCode = 403;
    }
    
    public AuthSecurityException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}