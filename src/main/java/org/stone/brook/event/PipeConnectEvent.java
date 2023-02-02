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

import org.stone.brook.Pipe;

/**
 * Server发现有新的连接后，触发事件
 *
 * @author Chris
 */
public class PipeConnectEvent extends PipeEvent {

    /**
     * Constructor with a source object.
     */
    public PipeConnectEvent(Pipe pipe) {
        super(pipe);
    }
}