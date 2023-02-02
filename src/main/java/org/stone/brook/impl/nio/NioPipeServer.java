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

import org.stone.brook.PipeListener;
import org.stone.brook.PipeServerListener;
import org.stone.brook.event.*;
import org.stone.brook.impl.BasePipeServer;
import org.stone.brook.impl.util.PipeUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Nio Server
 *
 * @author Chris Liao
 */
public class NioPipeServer extends BasePipeServer {

    /**
     * nio selector
     */
    private Selector serverSelector;

    /**
     * serverSocketChannel
     */
    private ServerSocketChannel serverSocketChannel;

    /**
     * 服务器等待接受线程
     */
    private NioPipeServerThread pipeServerThread;

    /**
     * 存放所有已经注册上的蔟
     */
    private List registeredPipeClusterList;

    /**
     * task timer
     */
    private Timer pipeClusterClearTimer;


    /**
     * 构造函数
     */
    public NioPipeServer(int port, PipeServerListener pipeServerListener, PipeListener pipeListener) throws IOException {
        super(port, pipeServerListener, pipeListener);
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.serverSelector = Selector.open();
        this.serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
        this.pipeServerThread = new NioPipeServerThread(this);
        this.handleEvent(new ServerCreatedEvent(port));

        this.registeredPipeClusterList = new ArrayList();
        this.pipeClusterClearTimer = new Timer(true);
        this.pipeClusterClearTimer.schedule(new ClearSelectorTask(this), 1000, 5000);
    }

    /**
     * 是否已经关闭
     */
    public boolean isClosed() {
        return this.serverSocketChannel.socket().isClosed();
    }

    /**
     * 是否处于运行状态
     */
    public boolean isListening() {
        return pipeServerThread.isAlive();
    }

    /**
     * return select key
     */
    public Selector getServerSelector() {
        return this.serverSelector;
    }

    /**
     * return select key
     */
    public ServerSocketChannel getServerSocketChannel() {
        return this.serverSocketChannel;
    }

    /**
     * 让服务器运行起来
     */
    public void startup() throws IOException {
        if (this.isListening()) {
            throw new SocketException("Server is running");
        } else {
            this.pipeServerThread.setDaemon(true);
            this.pipeServerThread.start();
        }
    }

    /**
     * 关闭Server
     */
    public void close() throws IOException {
        if (this.isClosed()) {
            throw new SocketException("Server is closed");
        } else {
            this.serverSocketChannel.close();
            this.clearEmptyCluster();
            this.pipeServerThread.interrupt();
            this.handleEvent(new ServerClosedEvent(this.getServerPort()));
        }
    }

    /**
     * 注册一个连接
     */
    void registerServerPipe(NioPipe pipe) throws IOException {
        NioPipeCluster targetCluster = null;
        Iterator itor = registeredPipeClusterList.iterator();
        while (itor.hasNext()) {
            NioPipeCluster cluster = (NioPipeCluster) itor.next();
            if (!cluster.isFull()) {
                targetCluster = cluster;
                break;
            }
        }

        if (targetCluster == null) {
            targetCluster = new NioPipeCluster();
            registeredPipeClusterList.add(targetCluster);
            targetCluster.setDaemon(true);
            targetCluster.start();
        }

        targetCluster.registerPipe(pipe);
    }

    /**
     * 注册一个连接
     */
    synchronized void clearEmptyCluster() {
        this.pipeClusterClearTimer.cancel();
        Iterator itor = registeredPipeClusterList.iterator();
        while (itor.hasNext()) {
            NioPipeCluster selector = (NioPipeCluster) itor.next();
            if (selector.isEmpty()) {
                selector.terminate();
                itor.remove();
            }
        }
    }

    /**
     * 检查是否存在空的Pipe selector,如果存在则关闭
     */
    private class ClearSelectorTask extends TimerTask {

        /**
         * 连接簇
         */
        private NioPipeServer pipeServer;

        /**
         * 构造函数
         */
        public ClearSelectorTask(NioPipeServer pipeServer) {
            this.pipeServer = pipeServer;
        }

        /**
         * 任务方法
         */
        public void run() {
            pipeServer.clearEmptyCluster();
        }
    }

    /**
     * Socket server thread
     */
    private class NioPipeServerThread extends Thread {

        /**
         * 父线程
         */
        private Thread parentThread;

        /**
         * 当前Thread需要监听的Server
         */
        private NioPipeServer pipeServer;

        /**
         * 构造函数
         */
        public NioPipeServerThread(NioPipeServer pipeServer) {
            this.pipeServer = pipeServer;
            this.parentThread = Thread.currentThread();
        }

        /**
         * Thread method
         */
        public void run() {
            NioPipe pipe = null;
            Selector serverSelector = pipeServer.getServerSelector();
            ServerSocketChannel serverChannel = pipeServer.getServerSocketChannel();
            while (this.parentThread.isAlive() && !pipeServer.isClosed()) {
                try {
                    if (serverSelector.select() > 0) {
                        Iterator itor = serverSelector.selectedKeys().iterator();
                        while (itor.hasNext()) {
                            SelectionKey key = (SelectionKey) itor.next();
                            itor.remove();
                            if (key.isAcceptable()) {
                                SocketChannel socketChannel = serverChannel.accept();
                                pipe = new NioPipe(socketChannel, pipeServer.getPipeListener(), pipeServer);
                                pipe.setReadBuffSize(pipeServer.getReadBuffSize());
                                pipe.setWriteBuffSize(pipeServer.getWriteBuffSize());

                                pipe.handleEvent(new PipeConnectEvent(pipe));
                                if (!pipe.isClosed()) {//管道没有被关闭的情况下，发布已连接事件，并让其处于运行
                                    pipe.handleEvent(new PipeConnectedEvent(pipe));
                                    pipeServer.registerServerPipe(pipe);
                                }
                            } else if (key.isConnectable()) {

                            }
                        }
                    }
                } catch (Throwable e) {
                    if (pipe != null) {
                        PipeUtil.close(pipe);
                        pipe.handleEvent(new PipeErrorEvent(pipe, e));
                        pipe.handleEvent(new PipeDisconnectedEvent(pipe));
                    }
                }
            }
        }
    }
}
