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

import org.stone.brook.PipeListener;
import org.stone.brook.PipeServer;
import org.stone.brook.PipeServerListener;
import org.stone.brook.event.ServerClosedEvent;
import org.stone.brook.event.ServerCreatedEvent;
import org.stone.brook.event.ServerErrorEvent;

import java.io.IOException;
import java.util.EventObject;

/**
 * 通讯Server
 *
 * @author Chris Liao
 */
public abstract class BasePipeServer implements PipeServer {

    /**
     * 服务器端口
     */
    private int serverPort;

    /**
     * 读缓存大小
     */
    private int readBuffSize = 1024;

    /**
     * 写缓存大小
     */
    private int writeBuffSize = 1024;

    /**
     * Socket事件处理者
     */
    private PipeListener pipeListener;

    /**
     * Server事件处理者
     */
    private PipeServerListener pipeServerListener;

    /**
     * 构造函数
     */
    public BasePipeServer(int serverPort, PipeServerListener pipeServerListener, PipeListener pipeListener) {
        this.serverPort = serverPort;
        this.pipeListener = pipeListener;
        this.pipeServerListener = pipeServerListener;
    }

    /**
     * 获得服务器端口
     */
    public int getServerPort() {
        return this.serverPort;
    }

    /**
     * Socket事件处理者
     */
    public PipeListener getPipeListener() {
        return pipeListener;
    }

    /**
     * Server事件处理者
     */
    public PipeServerListener getPipeServerListener() {
        return pipeServerListener;
    }

    /**
     * 获取读缓存区间大小
     */
    public int getReadBuffSize() {
        return readBuffSize;
    }

    /**
     * 设置读缓存区间大小
     */
    public void setReadBuffSize(int buffSize) throws IOException {
        if (buffSize <= 0) throw new IllegalArgumentException("Invalid read buffer size");
        this.readBuffSize = buffSize;
    }

    /**
     * 获取写缓存区间大小
     */
    public int getWriteBuffSize() {
        return writeBuffSize;
    }

    /**
     * 设置写缓存区间大小
     */
    public void setWriteBuffSize(int buffSize) throws IOException {
        if (buffSize <= 0) throw new IllegalArgumentException("Invalid write buffer size");
        this.writeBuffSize = buffSize;
    }

    /**
     * Implementation method from observer interface.
     */
    public void handleEvent(EventObject arg) {
        try {
            if (pipeServerListener != null) {
                if (arg instanceof ServerCreatedEvent) {
                    pipeServerListener.onCreated((ServerCreatedEvent) arg);
                } else if (arg instanceof ServerClosedEvent) {
                    pipeServerListener.onClosed((ServerClosedEvent) arg);
                } else if (arg instanceof ServerErrorEvent) {
                    pipeServerListener.onError((ServerErrorEvent) arg);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
