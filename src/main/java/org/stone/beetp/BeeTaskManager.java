/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import org.stone.beeop.BeeObjectPool;
import org.stone.beeop.BeeObjectSourceConfig;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Task Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeTaskManager extends BeeTaskManagerConfig {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private BeeObjectPool pool;
    private boolean ready;
    private Exception cause;

    //***************************************************************************************************************//
    //                                             1:constructors(2)                                                 //
    //***************************************************************************************************************//
    public BeeTaskManager() {
    }

    public BeeTaskManager(BeeObjectSourceConfig config) {

    }

    //***************************************************************************************************************//
    //                                        2: task submit methods(3)                                              //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws Exception {
        return null;
    }

    public List<BeeTaskHandle> submit(List<BeeTask> taskList) throws Exception {
        return null;
    }
}
