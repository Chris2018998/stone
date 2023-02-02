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

import java.io.IOException;

/**
 * 网络连接管道
 *
 * @author Chris Liao
 */
public interface Pipe {

    /**
     * 是否关闭
     */
    boolean isClosed();

    /**
     * 是否处于监听状态
     */
    boolean isListening();

    /**
     * 返回本地地址
     */
    PipeAddress getLocalHost();

    /**
     * 返回远程地址
     */
    PipeAddress getRemoteHost();

    /**
     * 设置读缓存区间大小
     */
    void setReadBuffSize(int buffSize) throws IOException;

    /**
     * 设置写缓存区间大小
     */
    void setWriteBuffSize(int buffSize) throws IOException;

    /**
     * 关闭连接
     */
    void close() throws IOException;

    /**
     * 保持监听
     */
    void keepListening() throws IOException;

    /**
     * 从连接上读出远程发送过来的数据
     */
    byte[] read() throws IOException;

    /**
     * 将数据通过连接输送给远程连接方
     */
    void write(byte[] data) throws IOException;

    /**
     * 将数据同步写给对方，并需要对方给出回复,默认超过时间为一小时
     */
    byte[] writeSyn(byte[] data) throws IOException;
}
