///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// *
// * Copyright(C) Chris2018998,All rights reserved.
// *
// * Project owner contact:Chris2018998@tom.com.
// *
// * Project Licensed under GNU Lesser General Public License v2.1.
// */
//package org.stone.shine.concurrent.locks.condition;
//
//import org.stone.shine.concurrent.locks.condition.threads.ReentrantLockConditionAwaitThread;
//import org.stone.shine.synchronizer.locks.ReentrantLock;
//import org.stone.test.TestCase;
//import org.stone.test.TestUtil;
//
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.LockSupport;
//
//import static org.stone.shine.concurrent.ConcurrentTimeUtil.ParkDelayNanos;
//
///**
// * lock condition test
// *
// * @author Chris Liao
// * @version 1.0
// */
//public class ReentrantLockSignalTest extends TestCase {
//
//    public void test() throws Exception {
//        //1:create lock and condition
//        ReentrantLock lock = new ReentrantLock();
//        Condition condition = lock.newCondition();
//
//        //2:create wait thread
//        ReentrantLockConditionAwaitThread conditionWaitThread = new ReentrantLockConditionAwaitThread(lock, condition, "await");
//        conditionWaitThread.start();
//
//        //3:lock in main thread
//        long signalTime;
//        LockSupport.parkNanos(ParkDelayNanos);
//        try {
//            lock.lock();
//            signalTime = System.currentTimeMillis();
//            condition.signal();
//        } finally {
//            lock.unlock();
//        }
//        //4:check time
//        LockSupport.parkNanos(ParkDelayNanos);
//        if (signalTime > conditionWaitThread.getAwaitOverTime()) TestUtil.assertError("Condition signal test failed");
//    }
//}
