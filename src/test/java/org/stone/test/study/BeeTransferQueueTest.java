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
import org.stone.tools.extension.BeeTransferQueue2;
import org.stone.tools.extension.BeeTransferQueueNode;

/**
 * Queue Test Case
 *
 * @author chris liao
 */
public class BeeTransferQueueTest {

    public static void main(String[] args) throws Exception {
        int threadSize = 1000;
        int loopSize = 10000;

        BeeTransferQueue queue1 = new BeeTransferQueue();
        BeeTransferQueue1TestThread[] threads1 = new BeeTransferQueue1TestThread[threadSize];
        for (int i = 0; i < threadSize; i++)
            threads1[i] = new BeeTransferQueue1TestThread(queue1, loopSize);
        long startTime1 = System.currentTimeMillis();
        for (int i = 0; i < threadSize; i++) threads1[i].start();
        for (int i = 0; i < threadSize; i++) threads1[i].join();
        System.out.println("Queue1 time:" + (System.currentTimeMillis() - startTime1) + "ms");

        BeeTransferQueue2 queue = new BeeTransferQueue2();
        BeeTransferQueue2TestThread[] threads = new BeeTransferQueue2TestThread[threadSize];
        for (int i = 0; i < threadSize; i++)
            threads[i] = new BeeTransferQueue2TestThread(queue, loopSize);
        long startTime2 = System.currentTimeMillis();
        for (int i = 0; i < threadSize; i++) threads[i].start();
        for (int i = 0; i < threadSize; i++) threads[i].join();
        System.out.println("Queue2 time:" + (System.currentTimeMillis() - startTime2) + "ms");
    }

    private static class BeeTransferQueue1TestThread extends Thread {
        private final int loopSize;
        private final BeeTransferQueue queue;

        public BeeTransferQueue1TestThread(BeeTransferQueue queue, int size) {
            this.queue = queue;
            this.loopSize = size;
        }

        public void run() {
            for (int i = 0; i < loopSize; i++) {
                BeeTransferQueueNode node = new BeeTransferQueueNode();
                queue.offer(node);
                queue.remove(node);
            }
        }
    }

    private static class BeeTransferQueue2TestThread extends Thread {
        private final int loopSize;
        private final BeeTransferQueue2 queue;

        public BeeTransferQueue2TestThread(BeeTransferQueue2 queue, int size) {
            this.queue = queue;
            this.loopSize = size;
        }

        public void run() {
            for (int i = 0; i < loopSize; i++) {
                BeeTransferQueueNode node = new BeeTransferQueueNode();
                queue.offer(node);
                queue.remove(node);
            }
        }
    }
}
