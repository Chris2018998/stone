/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.extension;

/**
 * Thread wait node,which is sharable class node to cross Semaphore to Transfer queue
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTransferQueueNode {
    //Node thread
    public Thread thread;
    //Node value,its initial value is null
    public volatile Object item;
    //Next node
    volatile BeeTransferQueueNode next;

    public BeeTransferQueueNode(Thread thread) {
        this.thread = thread;
    }

    public void clearThread() {
        this.thread = null;
    }
}
