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
public class Hello implements HelloMXBean {

    public UserInfo getUserInfo() {
        return new UserInfo("China", "Chris", 25);
    }
}
