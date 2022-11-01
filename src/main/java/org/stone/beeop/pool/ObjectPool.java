/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSourceConfig;

/**
 * object pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ObjectPool {

    //initialize pool with configuration
    void init(BeeObjectSourceConfig config) throws Exception;

    //borrow a object from pool
    BeeObjectHandle getObject() throws Exception;

    //recycle one pooled Connection
    void recycle(PooledObject entry);

    //remove failed object
    void abandonOnReturn(PooledObject entry);

    //close pool
    void close();

    //remove all pooled connections,if exists using connections,then wait util them idle,and close them and remove
    void clear();

    //clear all connections from pool,forceCloseUsingOnClear is true,then close using connection directly
    void clear(boolean forceCloseUsingOnClear);

    //check pool is closed
    boolean isClosed();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //get pool monitor vo
    ObjectPoolMonitorVo getPoolMonitorVo();

}
	
