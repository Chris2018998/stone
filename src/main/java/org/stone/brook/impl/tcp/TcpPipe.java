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

import org.stone.brook.PipeAddress;
import org.stone.brook.PipeListener;
import org.stone.brook.event.PipeConnectedEvent;
import org.stone.brook.event.PipeDataReadEvent;
import org.stone.brook.event.PipeDisconnectedEvent;
import org.stone.brook.event.PipeErrorEvent;
import org.stone.brook.impl.BasePipe;
import org.stone.brook.impl.ReadTimeoutException;
import org.stone.brook.impl.util.PipeUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * TCP pipe
 *
 * @author Chris
 */

public class TcpPipe extends BasePipe {

    /**
     * 通讯Socket
     */
    private Socket socket;

    /**
     * 管道本地地址
     */
    private PipeAddress localHost;

    /**
     * 管道连接的远程地址
     */
    private PipeAddress remoteHost;

    /**
     * 读等待线程
     */
    private TcpPipeReadThread pipeReadThread;

    /**
     * 构造函数
     */
    public TcpPipe(Socket socket, PipeListener listener) throws IOException {
        this(socket, listener, null);
        this.handleEvent(new PipeConnectedEvent(this));
    }

    /**
     * 构造函数
     */
    public TcpPipe(Socket socket, PipeListener listener, TcpPipeServer server) throws IOException {
        super(listener, server);
        this.socket = socket;
        InetAddress remote = socket.getInetAddress();
        InetAddress locale = socket.getLocalAddress();
        this.remoteHost = new PipeAddress(remote.getHostAddress(), remote.getHostName(), socket.getPort());
        this.localHost = new PipeAddress(locale.getHostAddress(), locale.getHostName(), socket.getLocalPort());
        this.pipeReadThread = new TcpPipeReadThread(this);
        this.pipeReadThread.setDaemon(true);
    }

    /**
     * 连接是否关闭
     */
    public boolean isClosed() {
        return this.socket.isClosed();
    }

    /**
     * 是否处于运行状态
     */
    public boolean isListening() {
        return pipeReadThread.isAlive();
    }

    /**
     * 管道本地地址
     */
    public PipeAddress getLocalHost() {
        return this.localHost;
    }

    /**
     * 管道连接的远程地址
     */
    public PipeAddress getRemoteHost() {
        return this.remoteHost;
    }

    /**
     * 设置读缓存区间大小
     */
    public void setReadBuffSize(int buffSize) throws IOException {
        if (isClosed()) throw new SocketException("Pipe has been closed,forbidden");
        if (this.isListening()) throw new SocketException("Pipe is in listening,forbidden");
        if (buffSize <= 0) throw new IllegalArgumentException("Invalid read buffer size");
        this.socket.setReceiveBufferSize(buffSize);
    }

    /**
     * 设置写缓存区间大小
     */
    public void setWriteBuffSize(int buffSize) throws IOException {
        if (isClosed()) throw new SocketException("Pipe has been closed,forbidden");
        if (this.isListening()) throw new SocketException("Pipe is in listening,forbidden");
        if (buffSize <= 0) throw new IllegalArgumentException("Invalid write buffer size");
        this.socket.setSendBufferSize(buffSize);
    }

    /**
     * 让连接运行起来
     */
    public void keepListening() throws IOException {
        if (this.isListening()) {
            throw new SocketException("Pipe is in listening");
        } else {
            this.pipeReadThread.start();
        }
    }

    /**
     * 关闭连接
     */
    public void close() throws IOException {
        if (this.isClosed()) {
            throw new SocketException("Pipe has been closed");
        } else {
            PipeUtil.close(this.socket);
            this.pipeReadThread.interrupt();
        }
    }

    /**
     * 发送数据到对方
     */
    public synchronized void write(byte[] data) throws IOException {
        try {
            if (data != null && data.length > 0) {
                int remainLen = data.length;
                int bufferSize = socket.getSendBufferSize();
                OutputStream stream = socket.getOutputStream();
                while (remainLen > 0) {
                    if (remainLen >= bufferSize) {
                        stream.write(data, data.length - remainLen, bufferSize);
                        stream.flush();
                        remainLen = remainLen - bufferSize;
                    } else {
                        stream.write(data, data.length - remainLen, remainLen);
                        stream.flush();
                        remainLen = 0;
                    }
                }
            }
        } catch (IOException e) {//对方关闭，将导致该异常
            this.close();
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeDisconnectedEvent(this));
            throw e;
        } catch (Throwable e) {
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeErrorEvent(this, e));
            throw new IOException(e.getMessage());
        }
    }

    /**
     * 从socket读取数
     */
    public synchronized byte[] read() throws IOException {
        try {
            int readLen = 0;
            InputStream stream = socket.getInputStream();
            byte[] readBuff = new byte[socket.getSendBufferSize()];
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            {
                readLen = stream.read(readBuff);
                if (readLen > 0) bytes.write(readBuff, 0, readLen);
            }
            while (readLen == readBuff.length) ;

            byte[] data = bytes.toByteArray();
            if (Thread.currentThread() == pipeReadThread) {
                PipeDataReadEvent event = new PipeDataReadEvent(this, data);
                this.handleEvent(event);
                byte[] replyData = event.getReplyData();
                if (replyData != null && replyData.length > 0 && !this.isClosed()) {
                    this.write(replyData);
                }
            }
            return data;
        } catch (SocketException e) {
            this.close();
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeDisconnectedEvent(this));
            throw e;
        } catch (java.io.EOFException e) {
            this.close();
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeDisconnectedEvent(this));
            throw e;
        } catch (IOException e) {
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeErrorEvent(this, e));
            throw e;
        } catch (Throwable e) {
            this.close();
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeDisconnectedEvent(this));
            throw new IOException(e.getMessage());
        }
    }

    /**
     * 在规定的时间范围内从连接上读出远程发送过来的数据,否则算超过时间
     */
    public synchronized byte[] read(int synWaitTime) throws IOException {
        int timeout = this.socket.getSoTimeout();
        try {
            this.socket.setSoTimeout(synWaitTime);
            return this.read();
        } catch (InterruptedIOException e) {
            throw new ReadTimeoutException();
        } catch (IOException e) {
            throw e;
        } finally {
            this.socket.setSoTimeout(timeout);
        }
    }

    /**
     * 连接的Thread
     *
     * @author Chris Liao
     */
    private class TcpPipeReadThread extends Thread {

        /**
         * 网络Socket
         */
        private TcpPipe pipe;

        /**
         * 父线程
         */
        private Thread parentThread;

        /**
         * 构造函数
         */
        public TcpPipeReadThread(TcpPipe pipe) {
            this.pipe = pipe;
            this.parentThread = Thread.currentThread();
        }

        /**
         * 线程方法
         */
        public void run() {
            while (this.parentThread.isAlive() && !this.pipe.isClosed()) {
                try {
                    pipe.read();
                } catch (Throwable e) {
                }
            }
        }
    }
}