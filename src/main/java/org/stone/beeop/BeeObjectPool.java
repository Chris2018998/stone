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
 * keyed object pool interface
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
    //                3: key methods(7)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    Object[] keys();

    void clear(Object key) throws Exception;

    void clear(Object key, boolean forceCloseUsing) throws Exception;

    void deleteKey(Object key) throws Exception;

    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

    BeeObjectPoolMonitorVo getPoolMonitorVo(Object key) throws Exception;

    void setPrintRuntimeLog(Object key, boolean indicator) throws Exception;

    //***************************************************************************************************************//
    //                4: Pool close(2)                                                                               //                                                                                  //
    //***************************************************************************************************************//
    void close();

    boolean isClosed();

    //***************************************************************************************************************//
    //                5: Pool clear(2)                                                                               //                                                                                  //
    //***************************************************************************************************************//
    //remove all objects from pool
    void clear(boolean forceCloseUsing) throws Exception;

    //remove all objects from pool
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;

    //***************************************************************************************************************//
    //                6: Pool log indicator and monitor(2)                                                                               //                                                                                  //
    //***************************************************************************************************************//
    void setPrintRuntimeLog(boolean indicator);

    BeeObjectPoolMonitorVo getPoolMonitorVo();

}