/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.countDownLatch;

import org.stone.test.TestCase;

/**
 * Count DownLatch Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CountDownLatchTest extends TestCase {

    public void test() throws Exception {

    }


//    public static void main(String[] args) throws Exception {
//        int count = 100;
//        int count2 = 1000;
//        CountDownLatch latch = new CountDownLatch(count);
//        CountDownThread[] threads = new CountDownThread[count];
//        CountWaitThread[] waits = new CountWaitThread[count2];
//        for (int i = 0; i < count; i++)
//            threads[i] = new CountDownThread(latch);
//        for (int i = 0; i < count2; i++)
//            waits[i] = new CountWaitThread(latch);
//        for (int i = 0; i < count; i++)
//            threads[i].start();
//        for (int i = 0; i < count2; i++)
//            waits[i].start();
//        //System.out.println("latch.count=" + latch.getCount());
//    }
//
//    static class CountDownThread extends Thread {
//        private CountDownLatch latch;
//
//        public CountDownThread(CountDownLatch latch) {
//            this.latch = latch;
//        }
//
//        public void run() {
//            latch.countDown();
//        }
//    }
//
//    static class CountWaitThread extends Thread {
//        private CountDownLatch latch;
//
//        public CountWaitThread(CountDownLatch latch) {
//            this.latch = latch;
//        }
//
//        public void run() {
//            try {
//                latch.await();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}
