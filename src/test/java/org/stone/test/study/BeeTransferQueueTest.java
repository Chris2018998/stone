/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.study;

import org.stone.tools.extension.BeeTransferQueue;
import org.stone.tools.extension.BeeTransferQueueNode;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue Test Case
 *
 * @author chris liao
 */
public class BeeTransferQueueTest {

    public static void main(String[] args) throws Exception {
        int threadSize = 10000;
        int loopSize = 100;

        ConcurrentLinkedQueue queue2 = new ConcurrentLinkedQueue();
        JDKQueueTestThread[] threads2 = new JDKQueueTestThread[threadSize];
        for (int i = 0; i < threadSize; i++)
            threads2[i] = new JDKQueueTestThread(queue2, loopSize);
        long startTime2 = System.currentTimeMillis();
        for (int i = 0; i < threadSize; i++) threads2[i].start();
        for (int i = 0; i < threadSize; i++) threads2[i].join();
        queue2.peek();
        System.out.println("JDK time:" + (System.currentTimeMillis() - startTime2) + "ms");

        BeeTransferQueue queue = new BeeTransferQueue();
        QueueTestThread[] threads = new QueueTestThread[threadSize];
        for (int i = 0; i < threadSize; i++)
            threads[i] = new QueueTestThread(queue, loopSize);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadSize; i++) threads[i].start();
        for (int i = 0; i < threadSize; i++) threads[i].join();
        System.out.println("Bee time:" + (System.currentTimeMillis() - startTime) + "ms");
    }

    private static class QueueTestThread extends Thread {
        private final int loopSize;
        private final BeeTransferQueue queue;

        public QueueTestThread(BeeTransferQueue queue, int size) {
            this.queue = queue;
            this.loopSize = size;
        }

        public void run() {
            for (int i = 0; i < loopSize; i++) {
                BeeTransferQueueNode node = new BeeTransferQueueNode(null);
                queue.offer(node);
                queue.remove(node);
            }
        }
    }

    private static class JDKQueueTestThread extends Thread {
        private final int loopSize;
        private final ConcurrentLinkedQueue queue;

        public JDKQueueTestThread(ConcurrentLinkedQueue queue, int size) {
            this.queue = queue;
            this.loopSize = size;
        }

        public void run() {
            for (int i = 0; i < loopSize; i++) {
                Object value = new Object();
                queue.offer(value);
                queue.remove(value);
            }
        }
    }
}
