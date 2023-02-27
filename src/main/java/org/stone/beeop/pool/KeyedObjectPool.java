/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import org.stone.beeop.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class KeyedObjectPool implements BeeObjectPool, BeeObjectPoolJmxBean {

    private Map<Object, ObjectGenericPool> subPoolMap = new ConcurrentHashMap<>();

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeObjectSourceConfig config) throws Exception {

    }

    //***************************************************************************************************************//
    //                2: objects methods(2)                                                                          //                                                                                  //
    //***************************************************************************************************************//
    //borrow a object from pool
    public BeeObjectHandle getObjectHandle() throws Exception {
        return getObjectHandle(null);
    }

    //borrow a object from pool
    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        ObjectGenericPool pool = subPoolMap.get(key);
        return null;
        //return pool.getObjectHandle();//@todo
    }

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //close pool
    public void close() {

    }

    //check pool is closed
    public boolean isClosed() {
        return false;
    }

    //get pool monitor vo
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    //enable Runtime Log
    public void setPrintRuntimeLog(boolean indicator) {

    }

    //remove all objects from pool
    public void restart(boolean forceCloseUsing) throws Exception {

    }

    //remove all objects from pool
    public void restart(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {

    }

    //***************************************************************************************************************//
    //                3: Jmx methods(6)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    //return current size(using +idle)
    public int getTotalSize() {
        //@todo
        return 0;
    }

    //return idle size
    public int getIdleSize() {
        //@todo
        return 0;
    }

    //return using size
    public int getUsingSize() {
        //@todo
        return 0;
    }

    //return semaphore acquired success size from pool
    public int getSemaphoreAcquiredSize() {
        //@todo
        return 0;
    }

    //return waiting size to take semaphore synchronizer
    public int getSemaphoreWaitingSize() {
        //@todo
        return 0;
    }

    //return waiter size for transferred object
    public int getTransferWaitingSize() {
        //@todo
        return 0;
    }
}
