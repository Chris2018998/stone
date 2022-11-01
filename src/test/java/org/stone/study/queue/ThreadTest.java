package org.stone.study.queue;

public class ThreadTest {
    public static void main(String[] args) {
        //Thread.currentThread().setDaemon(true);//failed
        Thread.currentThread().setPriority(1);//successful
        Thread.currentThread().setName("test");//successful
        new MyThread().start();
    }

    static class MyThread extends Thread {
        public void run() {
            System.out.println("getPriority:" + this.getPriority());
            System.out.println("isDaemon:" + this.isDaemon());
        }
    }
}
