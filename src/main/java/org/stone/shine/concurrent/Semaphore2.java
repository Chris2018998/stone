///*
// * Copyright(C) Chris2018998,All rights reserved
// *
// * Contact:Chris2018998@tom.com
// *
// * Licensed under GNU Lesser General Public License v2.1
// */
//package org.stone.stone;
//
//import PermitPool;
//
//import java.util.Collection;
//import java.util.concurrent.TimeUnit;
//
///**
// * Semaphore implementation
// *
// * @author Chris Liao
// * @version 1.0
// */
//public class Semaphore2 {
//    private PermitPool permitPool;
//
//    public Semaphore2(int permits) {
//        this(permits, false);
//    }
//
//    public Semaphore2(int size, boolean fair) {
//        this.permitPool = new PermitPool(size, fair);
//    }
//
//    public void acquire() throws InterruptedException {
//        //todo;
//    }
//
//    public void acquireUninterruptibly() {
//        //todo;
//    }
//
//    public boolean tryAcquire() {
//        return true;
//        //todo;
//    }
//
//    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
//        return true;
//        //todo;
//    }
//
//    public void release() {
//        //todo;
//    }
//
//
//    public void acquire(int permits) throws InterruptedException {
//        //todo;
//    }
//
//    public void acquireUninterruptibly(int permits) {
//        //todo;
//    }
//
//    public boolean tryAcquire(int permits) {
//        return true;
//        //todo;
//    }
//
//    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
//            throws InterruptedException {
//        return true;
//        //todo;
//    }
//
//    public void release(int permits) {
//
//    }
//
//    public int availablePermits() {
//        return 1;
//        //todo;
//    }
//
//    public int reducePermits(int reduction) {
//        return 1;
//        //todo;
//    }
//
//    public boolean isFair() {
//        return permitPool.isFair();
//    }
//
//    public final boolean hasQueuedThreads() {
//        return true;
//        //todo;
//    }
//
//    public final int getQueueLength() {
//        return 1;
//        //todo;
//    }
//
//    protected Collection<Thread> getQueuedThreads() {
//        return null;
//        //todo;
//    }
//}
