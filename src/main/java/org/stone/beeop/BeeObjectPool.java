/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop;

/**
 * object pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPool {

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void init(BeeObjectSourceConfig config) throws Exception;

    //***************************************************************************************************************//
    //                2: objects methods(2)                                                                          //                                                                                  //
    //***************************************************************************************************************//
    //borrow a object from pool
    BeeObjectHandle getObjectHandle() throws Exception;

    //borrow a object from pool
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //close pool
    void close();

    //check pool is whether closed
    boolean isClosed();

    //get pool monitor vo
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //remove all objects from pool
    void clear(boolean forceCloseUsing) throws Exception;

    //remove all objects from pool
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;
}