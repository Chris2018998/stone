/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.countDownLatch;

import org.jmin.stone.CountDownLatch2;

/**
 * Count DownLatch Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CountDownLatch2Test {
    public static void main(String[] args) throws Exception {
        int count = 20;
        CountDownLatch2 latch = new CountDownLatch2(count);
        CountDownThread[] threads = new CountDownThread[count];
        for (int i = 0; i < count; i++) {
            threads[i] = new CountDownThread(latch);
            threads[i].start();
        }

        latch.await();
        System.out.println("latch.count=" + latch.getCount());
    }

    static class CountDownThread extends Thread {
        private CountDownLatch2 latch;

        public CountDownThread(CountDownLatch2 latch) {
            this.latch = latch;
        }

        public void run() {
            latch.countDown();
        }
    }
}
