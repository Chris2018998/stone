/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.impl.udp;

import org.stone.brook.PipeAddress;
import org.stone.brook.PipeListener;
import org.stone.brook.event.PipeDataReadEvent;
import org.stone.brook.event.PipeErrorEvent;
import org.stone.brook.impl.BasePipe;
import org.stone.brook.impl.ReadTimeoutException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Udp pipe
 *
 * @author Chris
 */

public class UdpPipe extends BasePipe {

    /**
     * 管道本地地址
     */
    private PipeAddress localHost;

    /**
     * 管道连接的远程地址
     */
    private PipeAddress remoteHost;

    /**
     * 管道连接的远程地址
     */
    private InetAddress remoteAddress;

    /**
     * 数据读Socket
     */
    private DatagramSocket dataSocket;

    /**
     * 读等待线程
     */
    private Thread pipeReadThread;

    /**
     * 构造函数:客户端构造
     */
    public UdpPipe(InetAddress serverAddr, int serverPort, PipeListener pipeListener) throws IOException {
        super(pipeListener);
        this.dataSocket = new DatagramSocket();
        this.remoteAddress = serverAddr;
        InetAddress locale = dataSocket.getLocalAddress();
        this.localHost = new PipeAddress(locale.getHostAddress(), locale.getHostName(), dataSocket.getLocalPort());
        this.remoteHost = new PipeAddress(remoteAddress.getHostAddress(), remoteAddress.getHostName(), serverPort);//远程server
        this.dataSocket.connect(serverAddr, serverPort);
        this.pipeReadThread = new UdpPipeReadThread(this);
        this.pipeReadThread.setDaemon(true);
    }

    /**
     * 构造函数server端构造
     */
    public UdpPipe(DatagramSocket dataSocket, PipeListener pipeListener, UdpPipeServer pipeServer, Thread serverThread) throws IOException {
        super(pipeListener, pipeServer);
        this.dataSocket = dataSocket;
        this.pipeReadThread = serverThread;
        InetAddress locale = dataSocket.getLocalAddress();
        this.localHost = new PipeAddress(locale.getHostAddress(), locale.getHostName(), dataSocket.getLocalPort());
    }

    /**
     * 设置远程对象信息
     */
    void setRemoteHost(InetAddress remoteAddress, PipeAddress remoteHost) {
        this.remoteAddress = remoteAddress;
        this.remoteHost = remoteHost;
    }

    /**
     * 连接是否关闭
     */
    public boolean isClosed() {
        return this.dataSocket.isClosed();
    }

    /**
     * 是否处于运行状态
     */
    public boolean isListening() {
        return this.isServerSide() || pipeReadThread.isAlive();
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
        this.dataSocket.setReceiveBufferSize(buffSize);
    }

    /**
     * 设置写缓存区间大小
     */
    public void setWriteBuffSize(int buffSize) throws IOException {
        if (isClosed()) throw new SocketException("Pipe has been closed,forbidden");
        if (this.isListening()) throw new SocketException("Pipe is in listening,forbidden");
        if (buffSize <= 0) throw new IllegalArgumentException("Invalid write buffer size");
        this.dataSocket.setSendBufferSize(buffSize);
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
        if (!this.isServerSide()) {//如果是server端，则不允许关闭
            if (this.isClosed()) {
                throw new SocketException("Pipe has been closed");
            } else {
                this.dataSocket.close();
                this.pipeReadThread.interrupt();
            }
        }
    }

    /**
     * 发送数据到对方
     */
    public synchronized void write(byte[] data) throws IOException {
        try {
            if (data != null && data.length > 0) {
                int bufferSize = dataSocket.getSendBufferSize();
                if (data.length > bufferSize)
                    throw new IOException("Data length is out of buffer size:" + bufferSize);
                DatagramPacket packet = new DatagramPacket(data, data.length);
                packet.setAddress(remoteAddress);
                packet.setPort(remoteHost.getHostPort());
                dataSocket.send(packet);
            }
        } catch (Throwable e) {
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeErrorEvent(this, e));
            if (e instanceof IOException)
                throw (IOException) e;
            else
                throw new IOException(e.getMessage());
        }
    }

    /**
     * 从socket读取数
     */
    public synchronized byte[] read() throws IOException {
        try {
            byte[] dataReadBuff = new byte[dataSocket.getReceiveBufferSize()];
            DatagramPacket dataPacket = new DatagramPacket(dataReadBuff, dataReadBuff.length);
            dataSocket.receive(dataPacket);
            byte[] data = new byte[dataPacket.getLength()];
            System.arraycopy(dataReadBuff, 0, data, 0, data.length);
            if (Thread.currentThread() == pipeReadThread) {
                PipeDataReadEvent event = new PipeDataReadEvent(this, data);
                this.handleEvent(event);
                byte[] replyData = event.getReplyData();
                if (replyData != null && replyData.length > 0 && !this.isClosed()) {
                    this.write(replyData);
                }
            }
            return data;
        } catch (Throwable e) {
            if (Thread.currentThread() == pipeReadThread)
                this.handleEvent(new PipeErrorEvent(this, e));
            if (e instanceof IOException)
                throw (IOException) e;
            else
                throw new IOException(e.getMessage());
        }
    }

    /**
     * 在规定的时间范围内从连接上读出远程发送过来的数据,否则算超过时间
     */
    public synchronized byte[] read(int synWaitTime) throws IOException {
        int timeout = this.dataSocket.getSoTimeout();
        try {
            this.dataSocket.setSoTimeout(synWaitTime);
            return this.read();
        } catch (InterruptedIOException e) {
            throw new ReadTimeoutException();
        } catch (IOException e) {
            throw e;
        } finally {
            this.dataSocket.setSoTimeout(timeout);
        }
    }

    /**
     * pipe read thread
     */
    private class UdpPipeReadThread extends Thread {

        /**
         * 管道
         */
        private UdpPipe pipe;

        /**
         * 父线程
         */
        private Thread parentThread;

        /**
         * 构造函数
         */
        public UdpPipeReadThread(UdpPipe pipe) {
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
                    e.printStackTrace();
                }
            }
        }
    }
}