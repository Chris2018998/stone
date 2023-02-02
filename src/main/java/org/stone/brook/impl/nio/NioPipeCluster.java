/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.impl.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server pipe selector
 *
 * @author Chris Liao
 * @version 1.0
 */
public class NioPipeCluster extends Thread {

    /**
     * 最大可以注册64个连接
     */
    private final int MAX_SIZE = 64;
    /**
     * 注册的连接
     */
    private Map pipeMap;
    /**
     * nio的selector
     */
    private Selector selector;
    /**
     * 当前Thread的启动Thread
     */
    private Thread parentThread;
    /**
     * Thread是否终止
     */
    private boolean terminated;

    /**
     * 构造函数
     */
    public NioPipeCluster() throws IOException {
        this.pipeMap = new HashMap();
        this.selector = Selector.open();
        this.parentThread = Thread.currentThread();
    }

    /**
     * server channel reach max count?
     */
    public synchronized boolean isFull() {
        return pipeMap.size() == MAX_SIZE;
    }

    /**
     * no point registered
     */
    public synchronized boolean isEmpty() {
        return (pipeMap.size() == 0);
    }

    /**
     * 终止
     */
    public synchronized void terminate() {
        this.terminated = true;
        this.pipeMap.clear();
        this.selector.wakeup();
    }

    /**
     * 注册一个连接
     */
    public SelectionKey registerPipe(NioPipe pipe) throws IOException {
        SocketChannel socketChannel = pipe.getSocketChannel();
        socketChannel.configureBlocking(false);
        selector.wakeup();
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        pipe.setRegisterInfoFromServer(selector, selectionKey, this);
        this.pipeMap.put(selectionKey, pipe);
        return selectionKey;
    }

    /**
     * Thread启动方法
     */
    public void run() {
        while (!this.terminated && this.parentThread.isAlive()) {
            try {
                selector.select();
                if (this.terminated) break;
                Iterator itor = selector.selectedKeys().iterator();
                while (itor.hasNext()) {
                    SelectionKey key = (SelectionKey) itor.next();
                    itor.remove();

                    NioPipe pipe = (NioPipe) pipeMap.get(key);
                    if (pipe == null || pipe.isClosed()) {
                        pipeMap.remove(key);
                    } else {
                        try {
                            if (key.isReadable())
                                pipe.read();
                        } catch (IOException e) {
                            if (pipe.isClosed())
                                pipeMap.remove(key);
                        }
                    }
                }
            } catch (IOException e) {

            }
        }
    }
}