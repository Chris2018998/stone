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
 * Server创建事件
 *
 * @author Chris
 */
public class ServerCreatedEvent extends ServerEvent {

    /**
     * Constructor with a source object.
     */
    public ServerCreatedEvent(int port) {
        super(port);
    }
}