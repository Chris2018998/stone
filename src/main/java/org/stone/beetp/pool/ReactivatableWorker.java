/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

import org.stone.tools.atomic.IntegerFieldUpdaterImpl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Pool task worker(a draft Class)
 *
 * @author Chris Liao
 * @version 1.0
 */

final class ReactivatableWorker implements Runnable {
    private static final int BaseVal = 0xFFFF;
    private static final int MOVE_SHIFT = 16;
    private static final AtomicIntegerFieldUpdater<ReactivatableWorker> CtrlUpd = IntegerFieldUpdaterImpl.newUpdater(ReactivatableWorker.class, "workCtrl");

    //work thread created by thread factory
    private Thread workThread;
    //high-16:worker state,low-16:task count
    private volatile int workCtrl;
    //store tasks of this worker
    private ConcurrentLinkedQueue<BaseHandle> taskQueue;


    public ReactivatableWorker() {

    }


    //***************************************************************************************************************//
    //                                            IN/OUT tasks                                                       //
    //***************************************************************************************************************//

    /**
     * Pool call this method to push a task to worker.
     *
     * @param taskHandle is a handle passed from pool
     */
    void pushTask(BaseHandle taskHandle) {
        //1: offer task to queue
        taskQueue.offer(taskHandle);
        //2: increase the count of task offered into queue
        this.increaseTaskCount();
        //3:wake up work thread if in sleeping or create new thread to process task @todo
    }

    /**
     * This worker(maybe other workers) attempt to poll a task from private queue,if not task,then return null.
     *
     * @return a pulled out task
     */
    BaseHandle pollTask() {
        //poll a task from queue
        BaseHandle taskHandle = taskQueue.poll();
        //decrease the count of tasks in queue
        if (taskHandle != null) decreaseTaskCount();
        //return the pulled out task
        return taskHandle;
    }

    //***************************************************************************************************************//
    //                                            cas methods                                                        //
    //***************************************************************************************************************//
    private short increaseTaskCount() {
        for (; ; ) {
            int curControl = workCtrl;
            short taskCount = (short) (curControl & BaseVal);

            int newCtrl = curControl | (++taskCount & BaseVal);
            if (CtrlUpd.compareAndSet(this, curControl, newCtrl)) return taskCount;
        }
    }

    private short decreaseTaskCount() {
        for (; ; ) {
            int curControl = workCtrl;
            short taskCount = (short) (curControl & BaseVal);

            if (taskCount == 0) return taskCount;
            int newCtrl = curControl | (--taskCount & BaseVal);
            if (CtrlUpd.compareAndSet(this, curControl, newCtrl)) return taskCount;
        }
    }

    //***************************************************************************************************************//
    //                                             core method to process tasks                                      //
    //***************************************************************************************************************//
    public void run() {

    }

//    private static short getCount(int v) {
//        return (short) (v & BaseVal);
//    }
//
//    private static short getState(int v) {
//        return (short) (v >>> MOVE_SHIFT);
//    }
//
//    private static int build(short h, short l) {
//        return ((int) h << MOVE_SHIFT) | (l & BaseVal);
//    }
//
//    public static void main(String[] args) {
//        short l1 = getCount(Integer.MIN_VALUE);
//        int value1 = Integer.MIN_VALUE | (int)l1;
//
//        short l2 = getCount(Integer.MAX_VALUE);
//        int value2 = Integer.MAX_VALUE | (l2 & BaseVal);
//
//        System.out.println(value1 + " " + (value1 == Integer.MIN_VALUE));
//        System.out.println(value2 + " " + (value2 == Integer.MAX_VALUE));
//    }
}
