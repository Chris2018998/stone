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
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface KeyedObjectPool {

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    void init(BeeObjectSourceConfig config) throws Exception;


    //***************************************************************************************************************//
    //                2: key objects methods(5)                                                                      //                                                                                  //
    //***************************************************************************************************************//
    int keySize();

    Object[] keys();

    //remove key category from key pool
    void deleteKey(Object key);

    //remove keyed objects from pool
    void clearObjects(Object key, boolean forceCloseUsing);

    //borrow a keyed object from pool
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(5)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //close pool
    void close();

    //check pool is closed
    boolean isClosed();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //remove all objects from pool
    void restart(boolean forceCloseUsing) throws Exception;

    //remove all objects from pool
    void restart(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;
}
