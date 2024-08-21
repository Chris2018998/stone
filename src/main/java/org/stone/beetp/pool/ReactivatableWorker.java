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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pool task worker(a draft Class)
 *
 * @author Chris Liao
 * @version 1.0
 */

final class ReactivatableWorker implements Runnable {
    //work thread created by thread factory
    private Thread workThread;
    //high-16:worker state,low-16:task count
    private volatile int state;
    //store tasks of this worker
    private ConcurrentLinkedQueue<BaseHandle> taskQueue;

    public ReactivatableWorker() {

    }

    void changeState(int from, int to) {

    }

    private void increaseTaskCount() {

    }

    private void decreaseTaskCount() {

    }

    //***************************************************************************************************************//
    //                                            IN/OUT tasks                                                       //
    //***************************************************************************************************************//

    /**
     * this worker and other workers attempt to pull a task from queue,if queue is empty,then return null.
     *
     * @retur a pulled task
     */
    BaseHandle pullTask() {
        return null;
    }

    /**
     * Pool assign a task handle to this worker
     *
     * @param taskHandle is a handle passed from pool
     */
    void submitTask(BaseHandle taskHandle) {

    }

    //***************************************************************************************************************//
    //                                             core method to process tasks                                      //
    //***************************************************************************************************************//
    public void run() {

    }
}
