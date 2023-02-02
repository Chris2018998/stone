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
 * Server关闭事件
 *
 * @author Chris
 */
public class ServerClosedEvent extends ServerEvent {

    /**
     * Constructor with a source object.
     */
    public ServerClosedEvent(int port) {
        super(port);
    }
}