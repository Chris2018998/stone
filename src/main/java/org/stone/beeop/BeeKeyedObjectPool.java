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

import java.util.List;

/**
 * Keyed object pool interface.
 * <p>
 * Important Note: keys object are required to override three methods(equals, hashCode, toString).
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool<K, V> extends Cloneable {

    //***************************************************************************************************************//
    //                                     1: start and re-start(3)                                                  //
    //***************************************************************************************************************//

    /**
     * Pool startup with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied in pool
     * @throws Exception when fail to initialize
     */
    void start(BeeObjectSourceConfig<K, V> config) throws Exception;

    /**
     * Pool re-startup with last used configuration when pool is in ready state
     *
     * @param forceRecycleBorrowed is true that recycle borrowed connections immediately and make them return to pool;
     *                             false that wait them return to pool
     * @throws PoolInClearingException when pool is closed or in clearing
     */
    void restart(boolean forceRecycleBorrowed) throws Exception;

    /**
     * Pool re-startup with a new configuration when pool is in ready state.
     *
     * @param forceRecycleBorrowed is true that recycle borrowed connections immediately and make them return to pool;
     *                             false that wait them return to pool
     * @param config               is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInClearingException        when pool is closed or in clearing
     * @throws PoolInitializeFailedException  when fail to reinitialize
     */
    void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception;

    //***************************************************************************************************************//
    //                                     2: Pooled objects get(2)                                                  //
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
    //                                     3: Pool maintenance(2)                                                    //
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

    /**
     * Queries pool state whether is ready.
     *
     * @return true when pool is closed
     */
    boolean isReady();

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeObjectPoolMonitorVo}.
     *
     * @return monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * A switch method to enable or disable logs print of pool.
     *
     * @param enable is true that print, false not print
     */
    void enableLogPrint(boolean enable);

    /**
     * Interrupts waiting threads.
     *
     * @return interrupted threads
     * @throws Exception when key is null or not exist key in pool
     */
    List<Thread> interruptWaitingThreads() throws Exception;


    //***************************************************************************************************************//
    //                                        4: keys maintenance(4)                                                 //
    //***************************************************************************************************************//

    /**
     * Query key size of pool.
     *
     * @return an integer number
     */
    int keySize();

    /**
     * Query given key whether in pool.
     *
     * @param key to locate related pooled objects
     * @return a keys array
     */
    boolean exists(K key);

    /**
     * Only clear all pooled object related with given key and remain key in pool(different to {@link #deleteKey(Object)}).
     *
     * @param key to locate related pooled objects
     * @throws ObjectKeyException if key is null
     */
    void reset(K key) throws Exception;

    /**
     * Only clear all pooled object related with given key, and remain key in pool(different to {@link #deleteKey(Object, boolean)}).
     *
     * @param key                  to locate related pooled objects
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void reset(K key, boolean forceRecycleBorrowed) throws Exception;

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
     * A switch call to enable or disable logs print of pool.
     */
    boolean isEnabledLogPrint(K key) throws Exception;

    /**
     * A switch call to enable or disable logs print of pool.
     *
     * @param enable is true that print, false not print
     */
    void enableLogPrint(K key, boolean enable) throws Exception;

    /**
     * Interrupts waiting threads.
     *
     * @param key may be mapping to a set of pooled objects
     * @return interrupted threads
     * @throws Exception when key is null or not exist key in pool
     */
    List<Thread> interruptWaitingThreads(K key) throws Exception;

    /**
     * Get monitoring object contains some runtime info of keyed objects,for example:count of idle,using,creating,timeout and so on.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getMonitorVo(K key) throws Exception;

    //***************************************************************************************************************//
    //                                         5: method execution logs                                              //
    //***************************************************************************************************************//

    /**
     * Queries method log cache whether being enabled in pool.
     *
     * @return boolean true is enabled,false is disabled
     */
    boolean isEnabledMethodExecutionLogCache();

    /**
     * A switch method to enable or disable method log cache
     *
     * @param enable is true that make cache to collect method logs;false that make it to stop work
     */
    void enableMethodExecutionLogCache(boolean enable);

    /**
     * Set a new log handler to pool.
     *
     * @param listener to handle method logs
     */
    void setMethodExecutionListener(BeeMethodExecutionListener<K, V> listener);

    /**
     * Gets logs from pool with specified key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return a result list
     */
    List<BeeMethodExecutionLog<K, V>> getMethodExecutionLog(K key, int type);

    /**
     * Clears logs from pool with specified key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return a cleared list
     */
    List<BeeMethodExecutionLog<K, V>> clearMethodExecutionLog(K key, int type);

    /**
     * Gets All logs.
     *
     * @return a result list
     */
    List<BeeMethodExecutionLog<K, V>> getAllMethodExecutionLog(int type);

    /**
     * Clear All logs.
     *
     * @return a cleared list
     */
    List<BeeMethodExecutionLog<K, V>> clearAllMethodExecutionLog(int type);
}