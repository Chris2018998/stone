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

import org.stone.brook.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * UDP网络连接工厂
 *
 * @author Chris Liao
 */

public class UdpPipeFactory implements PipeFactory {

    /**
     * 连接到远程pipe
     */
    public Pipe connect(String serverHost, int serverPort, PipeListener pipeEventListener) throws IOException {
        return new UdpPipe(InetAddress.getByName(serverHost), serverPort, pipeEventListener);
    }

    /**
     * 在某个端口上建立一个网络UDP Server
     */
    public PipeServer createServer(int port, PipeServerListener serverEventListener, PipeListener pipeEventListener) throws IOException {
        return new UdpPipeServer(port, serverEventListener, pipeEventListener);
    }
}