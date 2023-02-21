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

import java.util.List;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface KeyedObjectPool {

    //initialize pool with configuration
    void init(BeeObjectSourceConfig config) throws Exception;

    //borrow a object from pool
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

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

    int keySize();

    List keyList();

    //remove key
    void removeKey(Object key);

}
