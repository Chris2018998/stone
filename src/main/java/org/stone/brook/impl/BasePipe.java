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

import org.stone.brook.Pipe;
import org.stone.brook.PipeListener;
import org.stone.brook.event.PipeConnectedEvent;
import org.stone.brook.event.PipeDataReadEvent;
import org.stone.brook.event.PipeDisconnectedEvent;
import org.stone.brook.event.PipeErrorEvent;

import java.io.IOException;
import java.util.EventObject;

/**
 * 通讯管道
 *
 * @author Chris Liao
 */
public abstract class BasePipe implements Pipe {

    /**
     * 默认读取消息的超时:10分钟
     */
    private final int DefaultTimeout = 180000;
    /**
     * 是否处于serverside
     */
    private boolean isServerSide;
    /**
     * 服务器
     */
    private BasePipeServer pipeServer;
    /**
     * 事件监听
     */
    private PipeListener pipeListener;

    /**
     * 构造函数
     */
    public BasePipe(PipeListener pipeListener) {
        this.isServerSide = false;
        this.pipeListener = pipeListener;
    }

    /**
     * 构造函数
     */
    public BasePipe(PipeListener pipeListener, BasePipeServer pipeServer) {
        if (pipeServer != null)
            this.isServerSide = true;
        this.pipeServer = pipeServer;
        this.pipeListener = pipeListener;
    }

    /**
     * 是否位于服务器端
     */
    public boolean isServerSide() {
        return this.isServerSide;
    }

    /**
     * 服务器
     */
    public BasePipeServer getPipeServer() {
        return this.pipeServer;
    }

    /**
     * 将数据通过连接输送给远程连接方
     */
    public synchronized byte[] read() throws IOException {
        throw new IOException("Not Implemented");
    }

    /**
     * 将数据通过连接输送给远程连接方
     */
    public synchronized byte[] read(int timeout) throws IOException {
        throw new IOException("Not Implemented");
    }

    /**
     * 将数据同步写给对方，并需要对方给出回复
     */
    public synchronized byte[] writeSyn(byte[] data) throws IOException {
        return this.writeSyn(data, DefaultTimeout);
    }

    /**
     * 将数据同步写给对方，并需要对方给出回复
     */
    public synchronized byte[] writeSyn(byte[] data, int timeout) throws IOException {
        this.write(data);
        return this.read(timeout);
    }

    /**
     * override method
     */
    public String toString() {
        return this.getRemoteHost().toString();
    }

    /**
     * Implementation method from observer interface.
     */
    public void handleEvent(EventObject evnet) {
        try {
            if (pipeListener != null) {
                if (evnet instanceof PipeConnectedEvent) {
                    pipeListener.onConnected((PipeConnectedEvent) evnet);
                } else if (evnet instanceof PipeDisconnectedEvent) {
                    pipeListener.onDisconnected((PipeDisconnectedEvent) evnet);
                } else if (evnet instanceof PipeDataReadEvent) {
                    pipeListener.onDataReadOut((PipeDataReadEvent) evnet);
                } else if (evnet instanceof PipeErrorEvent) {
                    pipeListener.onError((PipeErrorEvent) evnet);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            if (pipeServer != null)
                pipeServer.handleEvent(evnet);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}