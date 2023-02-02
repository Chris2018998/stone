/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.event;

import org.stone.brook.Pipe;

/**
 * 当监听在某个连接上读取到数据触发
 *
 * @author Chris
 */

public class PipeDataReadEvent extends PipeEvent {

    /**
     * receive data
     */
    private byte[] readData;

    /**
     * 将回复的数据
     */
    private byte[] replyData;

    /**
     * Constructor with a source object.
     */
    public PipeDataReadEvent(Pipe source, byte[] data) {
        super(source);
        this.readData = data;
    }

    /**
     * Return byte data
     *
     * @return
     */
    public byte[] getReadData() {
        return readData;
    }

    /**
     * 获得需要回复的数据
     */
    public byte[] getReplyData() {
        return this.replyData;
    }

    /**
     * 获得需要回复的数据
     */
    public void setReplyData(byte[] replyData) {
        this.replyData = replyData;
    }
}