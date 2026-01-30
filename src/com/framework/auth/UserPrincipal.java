package com.framework.auth;

import java.util.*;

public interface UserPrincipal {

    boolean isAuthenticated();
    boolean hasRole(String role);
    boolean hasAuthority(String authority);
    Object getUserId();
    String getUsername();
    default List<String> getRoles() {
        return Collections.emptyList();
    }
    default List<String> getAuthorities() {
        return Collections.emptyList();
    }
}