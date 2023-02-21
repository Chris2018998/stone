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
 * abstract pool
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class BaseObjectPool implements ObjectPool {

    //initialize pool with configuration
    public void init(BeeObjectSourceConfig config) throws Exception {
    }

    //borrow a object from pool
    public BeeObjectHandle getObjectHandle() throws Exception {
        return null;
    }

    //recycle one pooled Connection
    public void recycle(PooledObject entry) {
    }

    //remove failed object
    public void abandonOnReturn(PooledObject entry) {

    }

    //close pool
    public void close() {

    }

    //check pool is closed
    public boolean isClosed() {
        return false;
    }

    //enable Runtime Log
    public void setPrintRuntimeLog(boolean indicator) {

    }

    //get pool monitor vo
    public ObjectPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    //remove all objects from pool
    public void restart(boolean forceCloseUsing) throws Exception {

    }

    //remove all objects from pool
    public void restart(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {

    }
}
