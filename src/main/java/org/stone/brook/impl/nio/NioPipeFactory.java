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

import org.stone.brook.*;
import org.stone.brook.impl.util.PipeUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * 管道工厂
 *
 * @author Chris
 */
public class NioPipeFactory implements PipeFactory {

    /**
     * 连接到NIO Server
     */
    public Pipe connect(String serverHost, int serverPort, PipeListener pipeEventListener) throws IOException {
        if (PipeUtil.getJavaVersion() < 1.4)
            throw new UnsupportedClassVersionError("JRE version must be more than 1.4");
        else {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(serverHost, serverPort));
            while (!socketChannel.finishConnect()) ;
            if (socketChannel.isConnected()) {
                return new NioPipe(socketChannel, pipeEventListener);
            } else {
                throw new ConnectException("Connection refused: connect");
            }
        }
    }

    /**
     * 在某个端口上建立一个网络Server
     */
    public PipeServer createServer(int port, PipeServerListener serverEventListener, PipeListener pipeEventListener) throws IOException {
        if (PipeUtil.getJavaVersion() < 1.4)
            throw new UnsupportedClassVersionError("JRE version must be than 1.4");
        else {
            return new NioPipeServer(port, serverEventListener, pipeEventListener);
        }
    }
}
