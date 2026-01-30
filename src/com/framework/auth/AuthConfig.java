package com.framework.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration de l'authentification chargée depuis un fichier .properties
 */
public class AuthConfig {
    private String principalClass;
    private String sessionKey;
    private boolean anonymousEnabled;
    private String defaultRedirect;
    private String errorPage;
    private String loginPage;
    
    // Constructeur par défaut
    public AuthConfig() {
        this.sessionKey = "FRAMEWORK_AUTH_PRINCIPAL";
        this.anonymousEnabled = true;
        this.defaultRedirect = "/login";
        this.errorPage = "/error/403.jsp";
        this.loginPage = "/login.jsp";
    }
    
    // Getters et setters
    public String getPrincipalClass() {
        return principalClass;
    }
    
    public void setPrincipalClass(String principalClass) {
        this.principalClass = principalClass;
    }
    
    public String getSessionKey() {
        return sessionKey;
    }
    
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
    
    public boolean isAnonymousEnabled() {
        return anonymousEnabled;
    }
    
    public void setAnonymousEnabled(boolean anonymousEnabled) {
        this.anonymousEnabled = anonymousEnabled;
    }
    
    public String getDefaultRedirect() {
        return defaultRedirect;
    }
    
    public void setDefaultRedirect(String defaultRedirect) {
        this.defaultRedirect = defaultRedirect;
    }
    
    public String getErrorPage() {
        return errorPage;
    }
    
    public void setErrorPage(String errorPage) {
        this.errorPage = errorPage;
    }
    
    public String getLoginPage() {
        return loginPage;
    }
    
    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }
}
