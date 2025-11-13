/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.tools;

import org.stone.tools.extension.BeeTransferQueue;
import org.stone.tools.extension.BeeTransferQueueNode;

/**
 * Queue Test Case
 *
 * @author chris liao
 */
public class BeeTransferQueueTest {

    public static void main(String[] args) throws Exception {
        int threadSize = 10000;
        BeeTransferQueue queue = new BeeTransferQueue();
        QueueTestThread[] threads = new QueueTestThread[threadSize];
        for (int i = 0; i < threadSize; i++)
            threads[i] = new QueueTestThread(queue);
        for (int i = 0; i < threadSize; i++) threads[i].start();
        for (int i = 0; i < threadSize; i++) threads[i].join();

        //LockSupport.park();
    }

    private static class QueueTestThread extends Thread {
        private final BeeTransferQueue queue;

        public QueueTestThread(BeeTransferQueue queue) {
            this.queue = queue;
        }

        public void run() {
            BeeTransferQueueNode node = new BeeTransferQueueNode(this);
            queue.offer(node);
            queue.remove(node);
        }
    }
}
