/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.countDownLatch;

import org.stone.shine.concurrent.CountDownLatch;

/**
 * Count DownLatch Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CountDownLatch2Test {
    public static void main(String[] args) throws Exception {
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        CountDownThread[] threads = new CountDownThread[count];
        for (int i = 0; i < count; i++) {
            threads[i] = new CountDownThread(latch);
            threads[i].start();
        }
        latch.await();
        System.out.println("latch.count=" + latch.getCount());
    }

    static class CountDownThread extends Thread {
        private CountDownLatch latch;

        public CountDownThread(CountDownLatch latch) {
            this.latch = latch;
        }

        public void run() {
            latch.countDown();
        }
    }
}
