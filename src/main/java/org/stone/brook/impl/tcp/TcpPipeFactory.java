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

import org.stone.brook.*;

import java.io.IOException;
import java.net.Socket;

/**
 * 网络连接工厂
 *
 * @author Chris Liao
 */

public class TcpPipeFactory implements PipeFactory {

    /**
     * 连接到TCP Server
     */
    public Pipe connect(String serverHost, int serverPort, PipeListener listener) throws IOException {
        return new TcpPipe(new Socket(serverHost, serverPort), listener);
    }

    /**
     * 在某个端口上建立一个网络TCP Server
     */
    public PipeServer createServer(int port, PipeServerListener serverListener, PipeListener pipeEventListener) throws IOException {
        return new TcpPipeServer(port, serverListener, pipeEventListener);
    }
}