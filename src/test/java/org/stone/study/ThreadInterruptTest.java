package org.stone.study;

public class ThreadInterruptTest {

    public static void main(String[] args) throws Exception {
        Thread thread = Thread.currentThread();
        thread.interrupt();//set interruption flag when alive
        System.out.println("Is interrupted:" + thread.isInterrupted());
        thread.join(1000);//block failed
    }
}
