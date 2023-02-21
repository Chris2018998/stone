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
import org.stone.beeop.BeeObjectSourceConfig;

/**
 * Generic object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface GenericObjectPool {

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void init(BeeObjectSourceConfig config) throws Exception;


    //***************************************************************************************************************//
    //                2: objects methods(3)                                                                          //                                                                                  //
    //***************************************************************************************************************//
    //borrow a object from pool
    BeeObjectHandle getObjectHandle() throws Exception;

    //recycle one pooled Connection
    void recycle(PooledObject entry);

    //remove failed return object
    void abandonOnReturn(PooledObject entry);

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(5)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //close pool
    void close();

    //check pool is closed
    boolean isClosed();

    //clear timeout pooled objects
    void removeIdleTimeout();

    //
    int asyncRetryCount();

    //get pool monitor vo
    ObjectPoolMonitorVo getPoolMonitorVo();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //remove all objects from pool
    void restart(boolean forceCloseUsing) throws Exception;

    //remove all objects from pool
    void restart(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;

}
