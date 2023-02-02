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
 * 网络服务器
 *
 * @author Chris Liao
 */
public interface PipeServer {

    /**
     * 是否已经关闭
     */
    boolean isClosed();

    /**
     * 是否处于运行状态
     */
    boolean isListening();

    /**
     * 设置读缓存区间大小
     */
    void setReadBuffSize(int buffSize) throws IOException;

    /**
     * 设置写缓存区间大小
     */
    void setWriteBuffSize(int buffSize) throws IOException;

    /**
     * 关闭Server
     */
    void close() throws IOException;

    /**
     * 让Server运行起来，并处于等待接收客户的连接请求的状态
     */
    void startup() throws IOException;

}
