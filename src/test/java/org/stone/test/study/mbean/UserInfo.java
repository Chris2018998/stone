/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.study.mbean;

/**
 * Test Class
 *
 * @author chris liao
 */

public class UserInfo {
    private String country;
    private String username;
    private int userAge;

    public UserInfo(String country, String username, int userAge) {
        this.country = country;
        this.username = username;
        this.userAge = userAge;
    }

    public String getUsername() {
        return username;
    }

    public String getCountry() {
        return country;
    }

    public int getUserAge() {
        return userAge;
    }
}
