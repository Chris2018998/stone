/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.impl.tcp;

import org.stone.brook.PipeListener;
import org.stone.brook.PipeServerListener;
import org.stone.brook.event.*;
import org.stone.brook.impl.BasePipeServer;
import org.stone.brook.impl.util.PipeUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * TCP server定义
 *
 * @author Chris Liao
 */
public class TcpPipeServer extends BasePipeServer {

    /**
     * Server socket
     */
    private ServerSocket serverSocket;

    /**
     * server监听线程
     */
    private TcpPipeServerThread pipeServerThread;

    /**
     * 构造函数
     */
    public TcpPipeServer(int serverPort, PipeServerListener pipeServerListener, PipeListener pipeListener) throws IOException {
        super(serverPort, pipeServerListener, pipeListener);
        this.serverSocket = new ServerSocket(serverPort);
        this.pipeServerThread = new TcpPipeServerThread(this);
        this.pipeServerThread.setDaemon(true);
        this.handleEvent(new ServerCreatedEvent(serverPort));
    }

    /**
     * 服务端Socket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * 是否已经关闭
     */
    public boolean isClosed() {
        return this.serverSocket.isClosed();
    }

    /**
     * 是否处于运行状态
     */
    public boolean isListening() {
        return this.pipeServerThread.isAlive();
    }

    /**
     * 让服务器运行起来
     */
    public void startup() throws IOException {
        if (this.isListening()) {
            throw new SocketException("Server is running");
        } else {
            this.pipeServerThread.start();
        }
    }

    /**
     * 关闭Server
     */
    public void close() throws IOException {
        if (this.isClosed()) {
            throw new SocketException("Server has been closed");
        } else {
            PipeUtil.close(serverSocket);
            this.pipeServerThread.interrupt();
            this.handleEvent(new ServerClosedEvent(serverSocket.getLocalPort()));
        }
    }

    /**
     * Socket server under IO mode
     *
     * @author Chris
     */
    private class TcpPipeServerThread extends Thread {

        /**
         * 父线程
         */
        private Thread parentThread;

        /**
         * 当前Thread需要监听的socketServer
         */
        private TcpPipeServer pipeServer;

        /**
         * 构造函数
         */
        public TcpPipeServerThread(TcpPipeServer pipeServer) {
            this.pipeServer = pipeServer;
            this.parentThread = Thread.currentThread();
        }

        /**
         * Thread method
         */
        public void run() {
            TcpPipe pipe = null;
            ServerSocket serverSocket = pipeServer.getServerSocket();
            while (this.parentThread.isAlive() && !this.pipeServer.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    pipe = new TcpPipe(socket, pipeServer.getPipeListener(), pipeServer);
                    pipe.setReadBuffSize(pipeServer.getReadBuffSize());
                    pipe.setWriteBuffSize(pipeServer.getWriteBuffSize());
                    pipe.handleEvent(new PipeConnectEvent(pipe));
                    if (!pipe.isClosed()) {//管道没有被关闭的情况下，发布已连接事件，并让其处于运行
                        pipe.handleEvent(new PipeConnectedEvent(pipe));
                        pipe.keepListening();
                    }
                } catch (Throwable e) {
                    if (pipe != null) {
                        pipe.handleEvent(new PipeErrorEvent(pipe, e));
                        pipe.handleEvent(new PipeDisconnectedEvent(pipe));
                        PipeUtil.close(pipe);
                    }
                }
            }
        }
    }
}
