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
 * <p>
 * Important Note: keys object are required to override three methods(equals, hashCode, toString).
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool<K, V> {

    /**
     * Pool initialize with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied in pool
     * @throws Exception when fail to initialize
     */
    void init(BeeObjectSourceConfig<K, V> config) throws Exception;

    //***************************************************************************************************************//
    //                                    Object getting(2)                                                          //
    //***************************************************************************************************************//

    /**
     * Attempts to get an object from pool
     *
     * @return handle of borrowed object
     * @throws ObjectCreateException         when fail to create an object instance
     * @throws ObjectGetTimeoutException     when wait timeout in pool
     * @throws ObjectGetInterruptedException while waiting is interrupted
     */
    BeeObjectHandle<K, V> getObjectHandle() throws Exception;

    /**
     * Attempts to get an object from pool with a given category key.
     *
     * @param key is a category key which maybe mapping to a pooled objects or a group of objects
     * @return handle of borrowed object
     * @throws ObjectKeyException            when key is null or invalid, or category capacity is full
     * @throws ObjectCreateException         when fail to create an object instance
     * @throws ObjectGetTimeoutException     when wait timeout in pool
     * @throws ObjectGetInterruptedException while waiting is interrupted
     */
    BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception;

    //***************************************************************************************************************//
    //                                   Pool close(2)                                                               //
    //***************************************************************************************************************//

    /**
     * Shutdown pool to not work(closed state),closes all maintained connections and removes them from pool.
     */
    void close();

    /**
     * Queries pool whether is closed.
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    //***************************************************************************************************************//
    //                                         Pool Clean and monitoring                                             //
    //***************************************************************************************************************//

    /**
     * A switch call to enable or disable logs print in pool.
     *
     * @param enable is true that print, false not print
     */
    void setPrintRuntimeLog(boolean enable);

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeObjectPoolMonitorVo}.
     *
     * @return monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Physically closes all connections and removes them from pool which not accepts borrow requests before completion.
     *
     * @param forceRecycleBorrowed is true that recycle borrowed connections immediately and make them return to pool;
     *                             false that wait them return to pool
     * @throws PoolInClearingException when pool is closed or in clearing
     */
    void clear(boolean forceRecycleBorrowed) throws Exception;

    /**
     * Physically close all pooled objects and remove them from pool,then pool reinitialize with new configuration,not accept
     * requests before completion of this operation call.
     *
     * @param forceRecycleBorrowed is true that recycle borrowed connections immediately and make them return to pool;
     *                             false that wait them return to pool
     * @param config               is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInClearingException        when pool is closed or in clearing
     * @throws PoolInitializeFailedException  when fail to reinitialize
     */
    void clear(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception;


    //***************************************************************************************************************//
    //                                        keys maintenance                                                       //
    //***************************************************************************************************************//

    /**
     * Query key size in pool.
     *
     * @return an integer number
     */
    int keySize();

    /**
     * Query given key whether in pool.
     *
     * @return a keys array
     */
    boolean exists(K key);

    /**
     * Only clear all pooled object related with given key and remain key in pool(different to {@link #deleteKey(Object)}).
     *
     * @param key to locate related pooled objects
     * @throws ObjectKeyException if key is null
     */
    void clear(K key) throws Exception;

    /**
     * Only clear all pooled object related with given key, and remain key in pool(different to {@link #deleteKey(Object, boolean)}).
     *
     * @param key                  to locate related pooled objects
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void clear(K key, boolean forceRecycleBorrowed) throws Exception;

    /**
     * Delete a pooled key.
     *
     * @param key is a key to remove
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(K key) throws Exception;

    /**
     * Delete a pooled key.
     *
     * @param key                  is a key may map to a sub pool
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception;

    /**
     * Query print state of runtime logs.
     *
     * @param key pooled key
     * @return boolean value,true,keyed pool print runtime logs,otherwise not print
     * @throws Exception when key is null or not exist key in pool
     */
    boolean isPrintRuntimeLog(K key) throws Exception;

    /**
     * Enable runtime log print or disable.
     *
     * @param key    pooled key
     * @param enable is true,print logs;false,not print
     * @throws Exception when key is null or not exist key in pool
     */
    void setPrintRuntimeLog(K key, boolean enable) throws Exception;

    /**
     * Get monitoring object contains some runtime info of keyed objects,for example:count of idle,using,creating,timeout and so on.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getMonitorVo(K key) throws Exception;

    /**
     * Interrupts processing of object creation.
     *
     * @param key                  may be mapping to a set of pooled objects
     * @param onlyInterruptTimeout is true that only interrupt timeout creation,see{@link BeeObjectSourceConfig#getMaxWait()}
     * @return interrupted threads
     */
    Thread[] interruptObjectCreating(K key, boolean onlyInterruptTimeout) throws Exception;
}