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
 * Keyed object pool interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool {

    /**
     * Pool initialize with a configuration object.
     *
     * @param config is a configuration object defines some items applied in pool
     * @throws Exception when pool initializes fail
     */
    void init(BeeObjectSourceConfig config) throws Exception;

    //***************************************************************************************************************//
    //                                    Object getting(2)                                                          //
    //***************************************************************************************************************//

    /**
     * Attempts to get an object from pool with default category key.
     *
     * @return handle of borrowed object
     * @throws ObjectCreateException         when fail to create object instance
     * @throws ObjectGetTimeoutException     when wait timeout in pool,see{@link BeeObjectSourceConfig#getMaxWait()}
     * @throws ObjectGetInterruptedException while waiting interrupted
     */
    BeeObjectHandle getObjectHandle() throws Exception;

    /**
     * Attempts to get an object from pool with a given category key.
     *
     * @param key is a category key which maybe mapping to a pooled objects or a group of objects
     * @return handle of borrowed object
     * @throws ObjectKeyException            when key is null or invalid, or category capacity is full
     * @throws ObjectCreateException         when fail to create object instance
     * @throws ObjectGetTimeoutException     when wait timeout in pool,see{@link BeeObjectSourceConfig#getMaxWait()}
     * @throws ObjectGetInterruptedException while waiting interrupted
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    //***************************************************************************************************************//
    //                                   Pool close(2)                                                               //
    //***************************************************************************************************************//

    /**
     * Shutdown pool and make ti to be in closed state,all pooled objects are physically closed and removed from pool.
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
     * Get monitoring object bring out some runtime info of pool,detail refer to {@link BeeObjectPoolMonitorVo}
     *
     * @return monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Physically close all pooled objects and remove them from pool,not accept requests before completion of this operation call.
     *
     * @param forceRecycleBorrowed is that recycle borrowed connections immediately,false that wait borrowed connections released by callers
     * @throws PoolInClearingException       when pool already in clearing
     * @throws PoolInitializeFailedException when fail to reinitialize
     */
    void clear(boolean forceRecycleBorrowed) throws Exception;

    /**
     * Physically close all pooled objects and remove them from pool,then pool reinitialize with new configuration,not accept
     * requests before completion of this operation call.
     *
     * @param forceRecycleBorrowed is that recycle borrowed connections immediately,false that wait borrowed connections released by callers
     * @param config               is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInClearingException        when pool already in clearing
     * @throws PoolInitializeFailedException  when reinitialize failed
     */
    void clear(boolean forceRecycleBorrowed, BeeObjectSourceConfig config) throws Exception;


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
     * Only clear all pooled object related with given key and remain key in pool(different to {@link #deleteKey(Object)})
     *
     * @param key to locate related pooled objects
     * @throws ObjectKeyException if key is null
     */
    void clear(Object key) throws Exception;

    /**
     * Only clear all pooled object related with given key, and remain key in pool(different to {@link #deleteKey(Object, boolean)})
     *
     * @param key                  to locate related pooled objects
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void clear(Object key, boolean forceRecycleBorrowed) throws Exception;

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
     * @param key                  is a key may map to a sub pool
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(Object key, boolean forceRecycleBorrowed) throws Exception;

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