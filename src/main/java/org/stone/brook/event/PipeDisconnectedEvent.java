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
 * 当远程连接断开时候，触发的事件
 *
 * @author Chris
 */
public class PipeDisconnectedEvent extends PipeEvent {

    /**
     * Constructor with a source object.
     */
    public PipeDisconnectedEvent(Pipe pipe) {
        super(pipe);
    }
}