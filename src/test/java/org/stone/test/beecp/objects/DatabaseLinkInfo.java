/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects;

/**
 * @author Chris Liao
 */

public class DatabaseLinkInfo {
    private String JdbcUrl;
    private String username;
    private String password;

    public String getJdbcUrl() {
        return JdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.JdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
