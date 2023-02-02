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
 * 网络协议服对象工厂
 *
 * @author Chris Liao
 */
public interface PipeFactory {

    /**
     * 创建连接到Server的连接
     */
    Pipe connect(String serverHost, int serverPort, PipeListener pipeListener) throws IOException;

    /**
     * 在某个端口上建立一个网络Server
     */
    PipeServer createServer(int port, PipeServerListener pipeServerListener, PipeListener pipeListener) throws IOException;

}
