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

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPool;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class KeyedObjectPool implements BeeObjectPool {

    private Map<Object, CategoryPool> categoryMap = new ConcurrentHashMap<>();

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
        return null;
    }

    //borrow a object from pool
    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        return null;
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
}
