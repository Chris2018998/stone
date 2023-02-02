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
import org.stone.brook.PipeAddress;

import java.util.EventObject;

/**
 * 发生在网络连接器上的事件
 *
 * @author Chris Liao
 */
public class PipeEvent extends EventObject {

    /**
     * 构造函数
     */
    public PipeEvent(Pipe socket) {
        super(socket);
    }

    /**
     * 获得远程地址
     */
    public PipeAddress getRemoteHost() {
        return ((Pipe) super.getSource()).getRemoteHost();
    }
}