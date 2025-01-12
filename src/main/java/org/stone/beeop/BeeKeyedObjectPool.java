/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

import org.stone.beeop.pool.exception.*;

/**
 * Keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool {

    /**
     * Pool initialize with a configuration object
     *
     * @param config is a configuration object contains some parameter applied in pool
     * @throws Exception when pool initialize fail
     */
    void init(BeeObjectSourceConfig config) throws Exception;

    //***************************************************************************************************************//
    //                                    Object getting(2)                                                          //
    //***************************************************************************************************************//

    /**
     * Attempts to get an object instance from pool
     *
     * @return handle of borrowed object
     * @throws ObjectCreateException         when fail to create object instance
     * @throws ObjectGetTimeoutException     when wait timeout for a released object instance,see{@link BeeObjectSourceConfig#getMaxWait()}
     * @throws ObjectGetInterruptedException while an interruption occurs on waiting for a released object instance
     */
    BeeObjectHandle getObjectHandle() throws Exception;

    /**
     * Attempts to get an object instance from pool with a category key
     *
     * @param key to get pooled object
     * @return handle of borrowed object
     * @throws ObjectCreateException         when fail to create object instance
     * @throws ObjectGetTimeoutException     when wait timeout for a released object instance,see{@link BeeObjectSourceConfig#getMaxWait()}
     * @throws ObjectGetInterruptedException while an interruption occurs on waiting for a released object instance
     * @throws ObjectKeyException            when given key not exists in pool and count of pool category keys reach configured maximum
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    //***************************************************************************************************************//
    //                                   Pool close(2)                                                               //
    //***************************************************************************************************************//

    /**
     * Shut down pool,all pooled objects are closed and removed from pool;pool can't be reactivated after closed,
     * and rejects requests from borrowers.
     */
    void close();

    /**
     * Query pool state is closed
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    //***************************************************************************************************************//
    //                                         Pool Clean and monitoring                                             //
    //***************************************************************************************************************//

    /**
     * Enable runtime log print or disable
     *
     * @param enable is true that print, false not print
     */
    void setPrintRuntimeLog(boolean enable);

    /**
     * Get pool monitoring object contains some runtime info,for example:count of idle,using,creating,timeout and so on.
     *
     * @return monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Closes all pooled objects and remove them from pool;after completion of clean,pool reinitialize with used configuration
     *
     * @param forceCloseUsing is that close borrowed objects immediately or wait them return to pool,then close them
     * @throws PoolInClearingException       when pool already in clearing
     * @throws PoolInitializeFailedException when fail to reinitialize
     */
    void clear(boolean forceCloseUsing) throws Exception;

    /**
     * Closes all pooled objects and remove them from pool; after completion of clean,pool reinitialize with given configuration
     *
     * @param forceCloseUsing is true that close borrowed objects immediately;false that close borrowed objects when them return to pool
     * @param config          is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInClearingException        when pool already in clearing
     * @throws PoolInitializeFailedException  when reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;


    //***************************************************************************************************************//
    //                                        keys maintenance                                                       //
    //***************************************************************************************************************//

    /**
     * gets pooled keys
     *
     * @return a keys array
     */
    Object[] keys();

    /**
     * query given key is in pool
     *
     * @return a keys array
     */
    boolean exists(Object key);

    /**
     * Delete a pooled key
     *
     * @param key is a key to remove
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(Object key) throws Exception;

    /**
     * Delete a pooled key
     *
     * @param key             is a key may map to a sub pool
     * @param forceCloseUsing is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

    /**
     * Query print state of runtime logs
     *
     * @param key pooled key
     * @return boolean value,true,keyed pool print runtime logs,otherwise not print
     * @throws Exception when key is null or not exist key in pool
     */
    boolean isPrintRuntimeLog(Object key) throws Exception;

    /**
     * Enable runtime log print or disable
     *
     * @param key    pooled key
     * @param enable is true,print logs;false,not print
     * @throws Exception when key is null or not exist key in pool
     */
    void setPrintRuntimeLog(Object key, boolean enable) throws Exception;

    /**
     * Get monitoring object contains some runtime info of keyed objects,for example:count of idle,using,creating,timeout and so on.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception;

    /**
     * Interrupt activity of object creation in processing
     *
     * @param key                  may be mapping to a set of pooled objects
     * @param onlyInterruptTimeout is true that only interrupt timeout creation,see{@link BeeObjectSourceConfig#getMaxWait()}
     * @return interrupted threads
     */
    Thread[] interruptObjectCreating(Object key, boolean onlyInterruptTimeout) throws Exception;
}