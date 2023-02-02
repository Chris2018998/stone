/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.event;

/**
 * Server上发生异常
 *
 * @author Chris
 */
public class ServerErrorEvent extends ServerEvent {

    /**
     * caused exception
     */
    public Throwable detail;

    /**
     * 构造函数
     */
    public ServerErrorEvent(int serverPort, Throwable detail) {
        super(serverPort);
        this.detail = detail;
    }

    /**
     * Return caused throwable.
     */
    public Throwable getCause() {
        return detail;
    }

    /**
     * override method
     */
    public String toString() {
        return "Error occured in server port:" + this.getPort() + ",caused: " + detail.toString();
    }
}