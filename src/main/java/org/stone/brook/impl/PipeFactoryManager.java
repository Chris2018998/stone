/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.impl;

import org.stone.brook.PipeFactory;
import org.stone.brook.impl.nio.NioPipeFactory;
import org.stone.brook.impl.tcp.TcpPipeFactory;
import org.stone.brook.impl.udp.UdpPipeFactory;

/**
 * 管道工厂管理中心
 *
 * @author Chris
 */
public class PipeFactoryManager {

    /**
     * tcp管道工厂
     */
    private static PipeFactory tcpPipeFactory = new TcpPipeFactory();

    /**
     * nio格式管道工厂
     */
    private static PipeFactory nioPipeFactory = new NioPipeFactory();

    /**
     * udp格式管道工厂
     */
    private static PipeFactory udpPipeFactory = new UdpPipeFactory();

    /**
     * 通过协议查找管道工厂
     */
    public static PipeFactory getPipeFactory(String mode) {
        if ("tcp".equalsIgnoreCase(mode)) {
            return tcpPipeFactory;
        } else if ("nio".equalsIgnoreCase(mode)) {
            return nioPipeFactory;
        } else if ("udp".equalsIgnoreCase(mode)) {
            return udpPipeFactory;
        } else {
            throw new RuntimeException("Not support protocol:" + mode);
        }
    }
}
