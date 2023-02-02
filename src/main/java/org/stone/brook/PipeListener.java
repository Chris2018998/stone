/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook;

import org.stone.brook.event.PipeConnectedEvent;
import org.stone.brook.event.PipeDataReadEvent;
import org.stone.brook.event.PipeDisconnectedEvent;
import org.stone.brook.event.PipeErrorEvent;

/**
 * 网络连接器上的事件监听器
 *
 * @author Chris Liao
 */
public interface PipeListener {

    /**
     * When connection request coming,this method will be called
     */
    void onConnected(PipeConnectedEvent event);

    /**
     * Method run after remote host close connection.
     */
    void onDisconnected(PipeDisconnectedEvent event);

    /**
     * Read data from remote host
     */
    void onDataReadOut(PipeDataReadEvent event);

    /**
     * When errors occur during communication
     */
    void onError(PipeErrorEvent event);

}
