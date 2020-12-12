package com.rovoq.electio.domain;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    USER, ADMIN;

    public String getAuthority() {
        return name(); // Строковое представление значения enum
    }
}
