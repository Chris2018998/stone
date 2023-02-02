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

import org.stone.brook.PipeAddress;
import org.stone.brook.PipeListener;
import org.stone.brook.event.PipeDataReadEvent;
import org.stone.brook.event.PipeDisconnectedEvent;
import org.stone.brook.event.PipeErrorEvent;
import org.stone.brook.impl.BasePipe;
import org.stone.brook.impl.ReadTimeoutException;
import org.stone.brook.impl.util.PipeUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * NIO pipe
 *
 * @author Chris
 */

public class NioPipe extends BasePipe {

    /**
     * socket
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
     * registered select key
     */
    private SocketChannel socketChannel;

    /**
     * registered select key
     */
    private SelectionKey selectionKey;

    /**
     * Selector
     */
    private Selector socketSelector;

    /**
     * 读等待线程
     */
    private Thread pipeReadThread;

    /**
     * 构造函数
     */
    public NioPipe(SocketChannel socketChannel, PipeListener pipeListener) throws IOException {
        this(socketChannel, pipeListener, null);
        this.socketSelector = Selector.open();
    }

    /**
     * 构造函数
     */
    public NioPipe(SocketChannel socketChannel, PipeListener pipeListener, NioPipeServer pipeServer) throws IOException {
        super(pipeListener, pipeServer);
        this.socketChannel = socketChannel;
        this.socket = this.socketChannel.socket();
        InetAddress remote = socket.getInetAddress();
        InetAddress locale = socket.getLocalAddress();
        this.remoteHost = new PipeAddress(remote.getHostAddress(), remote.getHostName(), socket.getPort());
        this.localHost = new PipeAddress(locale.getHostAddress(), locale.getHostName(), socket.getLocalPort());
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
        return (pipeReadThread != null && pipeReadThread.isAlive());
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
     * getSocketChannel
     */
    SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * set data thread
     */
    void setRegisterInfoFromServer(Selector selector, SelectionKey selectionKey, Thread pipeReadThread) {
        this.socketSelector = selector;
        this.selectionKey = selectionKey;
        this.pipeReadThread = pipeReadThread;
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
            if (!this.isServerSide()) {//位于客户端
                this.socketChannel.configureBlocking(false);
                this.selectionKey = this.socketChannel.register(socketSelector, SelectionKey.OP_READ);
                this.pipeReadThread = new NioPipeClientThread(this, socketSelector);
                this.pipeReadThread.setDaemon(true);
                this.pipeReadThread.start();
            }
        }
    }

    /**
     * 关闭连接
     */
    public void close() throws IOException {
        if (this.isClosed()) {
            throw new SocketException("Pipe has been closed");
        } else {
            try {
                this.socketChannel.close();
            } catch (IOException e) {
            }

            if (selectionKey != null)
                this.selectionKey.cancel();
            if (this.socketSelector != null && !this.isServerSide())//客户端唤醒
                this.socketSelector.wakeup();
        }
    }

    /**
     * 发送数据到对方
     */
    public synchronized void write(byte[] data) throws IOException {
        try {
            if (data != null && data.length > 0) {
                int remainLen = data.length;
                int buffsize = socketChannel.socket().getSendBufferSize();
                ByteBuffer dataBuffer = ByteBuffer.allocateDirect(buffsize);

                while (remainLen > 0) {
                    if (remainLen >= buffsize) {
                        dataBuffer.put(data, data.length - remainLen, buffsize);
                        dataBuffer.flip();
                        socketChannel.write(dataBuffer);
                        socketChannel.socket().getOutputStream().flush();
                        dataBuffer.clear();
                        remainLen = remainLen - buffsize;
                    } else {
                        dataBuffer.put(data, data.length - remainLen, remainLen);
                        dataBuffer.flip();
                        socketChannel.write(dataBuffer);
                        socketChannel.socket().getOutputStream().flush();
                        dataBuffer.clear();
                        remainLen = remainLen - buffsize;
                        remainLen = 0;
                    }
                }
                socketChannel.socket().getOutputStream().flush();
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
            int bufSize = socket.getReceiveBufferSize();
            ByteBuffer readBuff = ByteBuffer.allocate(bufSize);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int readLen = socketChannel.read(readBuff);
            if (readLen > 0) bytes.write(readBuff.array(), 0, readLen);
            while (readLen == bufSize) {//满读，说明后面有可能还有数据
                readLen = socketChannel.read(readBuff);
                if (readLen > 0) bytes.write(readBuff.array(), 0, readLen);
            }

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
        } catch (IOException e) {
            this.close();
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeDisconnectedEvent(this));
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
        try {
            int keyCount = this.socketSelector.select(synWaitTime);
            if (keyCount == 0) {
                throw new ReadTimeoutException();
            } else {
                return this.read();
            }
        } catch (InterruptedIOException e) {
            throw new ReadTimeoutException();
        } catch (IOException e) {
            throw e;
        }
    }


    /**
     * 如果是pipe位于客户端,则采用新该线程等待
     */
    private class NioPipeClientThread extends Thread {

        /**
         * pipe
         */
        private NioPipe nioPipe;

        /**
         * selector
         */
        private Selector selector;

        /**
         * 父线程
         */
        private Thread parentThread;

        /**
         * 构造函数
         */
        public NioPipeClientThread(NioPipe nioPipe, Selector selector) {
            this.nioPipe = nioPipe;
            this.selector = selector;
            this.parentThread = Thread.currentThread();
        }

        /**
         * 线程方法
         */
        public void run() {
            while (this.parentThread.isAlive() && this.nioPipe != null && !this.nioPipe.isClosed()) {
                try {
                    selector.select();
                    Iterator itor = selector.selectedKeys().iterator();
                    while (itor.hasNext()) {
                        SelectionKey key = (SelectionKey) itor.next();
                        itor.remove();
                        if (key.isReadable()) {
                            try {
                                nioPipe.read();
                            } catch (IOException e) {
                                if (nioPipe != null) {
                                    key.cancel();
                                    PipeUtil.close(nioPipe);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
