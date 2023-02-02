/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.impl;

import java.io.IOException;

/**
 * 读超时异常
 *
 * @author Chris
 */
public class ReadTimeoutException extends IOException {

    /**
     * 构造函数
     */
    public ReadTimeoutException() {
        super();
    }

    /**
     * 构造函数
     */
    public ReadTimeoutException(String s) {
        super(s);
    }
}