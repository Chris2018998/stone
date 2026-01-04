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

import org.stone.beeop.exception.*;

import java.util.List;

/**
 * Object keyed pool interface.
 *
 * @param <K> is pooled key type
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool<K, V> extends AutoCloseable {

    //***************************************************************************************************************//
    //                                     1: Pooled objects get(2)                                                  //
    //***************************************************************************************************************//

    /**
     * Attempts to get an object of default category from pool.
     *
     * @return handle of borrowed object
     * @throws ObjectCreatedException        when fail to create an object instance
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
     * @throws ObjectCreatedException        when fail to create an object instance
     * @throws ObjectGetTimeoutException     when wait timeout in pool
     * @throws ObjectGetInterruptedException while waiting is interrupted
     */
    BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception;

    //***************************************************************************************************************//
    //                                     2: Pooled keys maintenance(8)                                             //
    //***************************************************************************************************************//

    /**
     * Query size of pooled keys.
     *
     * @return an integer number
     */
    int keySize() throws Exception;

    /**
     * Query given key whether is pooled.
     *
     * @param key to locate related pooled objects
     * @return a keys array
     */
    boolean exists(K key) throws Exception;

    /**
     * Suspend key when key is ready,all borrow requests on key rejects
     *
     * @return true when success
     */
    boolean suspendKey(K key) throws Exception;

    /**
     * resume key when key is suspended
     *
     * @return true when success
     */
    boolean resumeKey(K key) throws Exception;

    /**
     * Clears pooled objects with given key.
     *
     * @param key is a key to search related pooled objects
     * @throws ObjectKeyException if key is null
     */
    void clearObjects(K key) throws Exception;

    /**
     * Clears pooled objects with given key.
     *
     * @param key                  to locate related pooled objects
     * @param forceRecycleBorrowed is true,close using objects directly;false that pool waiting using objects return to pool and then close it by physically.
     * @throws ObjectKeyException if key is null or default
     */
    void clearObjects(K key, boolean forceRecycleBorrowed) throws Exception;

    /**
     * Deletes given pooled key from pool.
     *
     * @param key is a key to remove
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(K key) throws Exception;

    /**
     * Deletes given pooled key from pool.
     *
     * @param key                  is a key may map to a sub pool
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null or default
     */
    void deleteKey(K key, boolean forceRecycleBorrowed) throws Exception;

    //***************************************************************************************************************//
    //                                     3: Pool maintenance (7)                                                   //
    //***************************************************************************************************************//

    /**
     * Shutdown pool.
     */
    void close();

    /**
     * Queries pool whether is closed.
     *
     * @return true if pool closed,otherwise return false
     */
    boolean isClosed();

    /**
     * Suspend pool when pool is ready,then pool rejects all borrow requests.
     *
     * @return true when success
     */
    boolean suspendPool();

    /**
     * resume pool when pool is suspended
     *
     * @return true when success
     */
    boolean resumePool();

    /**
     * Pool start with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied in pool
     * @throws Exception when fail to initialize
     */
    void start(BeeObjectSourceConfig<K, V> config) throws Exception;

    /**
     * Pool restart with last used configuration.
     *
     * @param forceRecycleBorrowed is true that force borrowed objects return to pool immediately; false that wait them return to pool
     * @throws BeeObjectSourcePoolRestartedException when pool is closed or in clearing
     */
    void restart(boolean forceRecycleBorrowed) throws Exception;

    /**
     * Pool restart with a new configuration.
     *
     * @param forceRecycleBorrowed is true that force borrowed objects return to pool immediately; false that wait them return to pool
     * @param config               is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException        when config is null
     * @throws BeeObjectSourcePoolRestartedException when pool is closed or in clearing
     * @throws BeeObjectSourcePoolStartedException   when fail to reinitialize
     */
    void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception;

    //***************************************************************************************************************//
    //                                     4: Pool Log Print(2)                                                      //
    //***************************************************************************************************************//

    /**
     * A switch method to enable or disable logs print of pool and apply change on all pooled category keys.
     *
     * @param enable is true that print, false not print
     */
    void enableLogPrint(boolean enable);

    /**
     * A switch method to enable or disable logs print flag on target pooled key.
     *
     * @param enable is true that print, false not print
     */
    void enableLogPrint(K key, boolean enable) throws Exception;

    //***************************************************************************************************************//
    //                                     5: Pool Monitoring(2)                                                     //
    //***************************************************************************************************************//

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeObjectKeyMonitorVo}.
     *
     * @param keyMonitor is true,then get monitor info of keys
     * @return monitor of pool
     */
    BeeObjectKeyPoolMonitorVo<K> getPoolMonitorVo(boolean keyMonitor);

    /**
     * Get monitoring info by pooled key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) throws Exception;

    //***************************************************************************************************************//
    //                                     6: Pool blocking interrupts(2)                                            //
    //***************************************************************************************************************//

    /**
     * Interrupts waiting threads in pool.
     *
     * @return interrupted threads
     */
    List<Thread> interruptWaitingThreads();

    /**
     * Interrupts waiting threads on pooled key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return interrupted threads
     * @throws Exception when key is null or not exist key in pool
     */
    List<Thread> interruptWaitingThreads(K key) throws Exception;

    //***************************************************************************************************************//
    //                                     7: Method execution logs(7)                                               //
    //***************************************************************************************************************//

    /**
     * A switch method to enable or disable method log cache.
     *
     * @param enable is true that make cache to collect method logs;false that make it to stop work
     */
    void enableMethodExecutionLogCache(boolean enable);

    /**
     * Set a new log listener to pool.
     *
     * @param listener to handle method logs
     */
    void setMethodExecutionListener(BeeMethodExecutionListener<K> listener);

    /**
     * Gets logs with given type.
     *
     * @param type should be one of[BeeMethodExecutionLog.Type_Object_Get,BeeMethodExecutionLog.Type_Object_Call];if not,then clear all logs
     * @return a result list
     */
    List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(int type);

    /**
     * Clear logs with given type.
     *
     * @param type should be one of[BeeMethodExecutionLog.Type_Object_Get,BeeMethodExecutionLog.Type_Object_Call];if not,then clear all logs
     * @return a cleared list
     */
    List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(int type);

    /**
     * Gets logs from pool with specified key.
     *
     * @param key  is category key,null value is not acceptable
     * @param type should be one of[BeeMethodExecutionLog.Type_Object_Get,BeeMethodExecutionLog.Type_Object_Call];if not,then get all logs
     * @return a result list
     */
    List<BeeMethodExecutionLog<K>> getMethodExecutionLogs(K key, int type);

    /**
     * Clears logs from pool with specified key.
     *
     * @param key  is category key,null value is not acceptable
     * @param type should be one of[BeeMethodExecutionLog.Type_Object_Get,BeeMethodExecutionLog.Type_Object_Call];if not,then clear all logs
     * @return a cleared list
     */
    List<BeeMethodExecutionLog<K>> clearMethodExecutionLogs(K key, int type);
}