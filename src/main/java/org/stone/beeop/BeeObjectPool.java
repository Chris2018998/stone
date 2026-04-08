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
 * Object keyed pool interface,whose instance works as an internal pool in {@link BeeObjectSource}.
 *
 * @param <K> is pooled key type
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPool<K, V> extends AutoCloseable {

    //***************************************************************************************************************//
    //                                     1: Pool maintenance (6)                                                   //
    //***************************************************************************************************************//

    /**
     * Shutdown pool.
     */
    void close();

    /**
     * Suspend pool when pool is ready,then pool rejects all borrow requests.
     *
     * @return true when pool suspend successful
     */
    boolean suspend() throws Exception;

    /**
     * resume pool when pool is suspended
     *
     * @return true when pool resume to ready from suspended state
     */
    boolean resume() throws Exception;

    /**
     * Pool starts with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied in pool
     * @throws BeeObjectSourceConfigException             when parameter config is null or it checks failed
     * @throws BeeObjectSourcePoolStartedFailureException when pool starts failed
     */
    void start(BeeObjectSourceConfig<K, V> config) throws Exception;

    /**
     * Pool restart with last used configuration.
     *
     * @param forceRecycleBorrowed is true that force borrowed objects return to pool immediately; false that wait them return to pool
     * @throws BeeObjectSourcePoolStartedFailureException when pool restarts failed
     */
    void restart(boolean forceRecycleBorrowed) throws Exception;

    /**
     * Pool restart with a new configuration.
     *
     * @param forceRecycleBorrowed is true that pool physically closes all pooled objects,false that pool wait borrowed objects return to pool
     * @param config               is a new configuration for pool restarting
     * @throws BeeObjectSourceConfigException             when parameter config is null or it checks failed
     * @throws BeeObjectSourcePoolStartedFailureException when pool restarts failed
     */
    void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) throws Exception;

    //***************************************************************************************************************//
    //                                     2: Pooled objects get(2)                                                  //
    //***************************************************************************************************************//

    /**
     * Attempts to get an object of default category from pool.
     *
     * @return handle of borrowed object
     * @throws BeePooledObjectCreationException       when fail to create an object instance
     * @throws BeePooledObjectGetTimeoutException     when wait timeout in pool
     * @throws BeePooledObjectGetInterruptedException while waiting is interrupted
     */
    BeeObjectHandle<K, V> getObjectHandle() throws Exception;

    /**
     * Attempts to get an object from pool with a given category key.
     *
     * @param key is a category key which maybe mapping to a pooled objects or a group of objects
     * @return handle of borrowed object
     * @throws BeePooledObjectKeyException            when key is null or invalid, or category capacity is full
     * @throws BeePooledObjectCreationException       when fail to create an object instance
     * @throws BeePooledObjectGetTimeoutException     when wait timeout in pool
     * @throws BeePooledObjectGetInterruptedException while waiting is interrupted
     */
    BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception;

    //***************************************************************************************************************//
    //                                     3: Pooled keys maintenance(8)                                             //
    //***************************************************************************************************************//

    /**
     * Query size of pooled keys.
     *
     * @return an integer number
     */
    int keySize() throws Exception;

    /**
     * Query given key whether exists in pool.
     *
     * @param key to locate related pooled objects
     * @return a keys array
     */
    boolean existsKey(K key) throws Exception;

    /**
     * Suspend key when it is ready, pool rejects all borrow requests to key
     *
     * @return true when suspend successful
     */
    boolean suspendKey(K key) throws Exception;

    /**
     * Resume key when it is suspended
     *
     * @return true when success
     */
    boolean resumeKey(K key) throws Exception;

    /**
     * Clears pooled objects mapping to given key.
     *
     * @param key is a key to search related pooled objects
     * @throws BeePooledObjectKeyException if key is null
     */
    void clearKeyObjects(K key) throws Exception;

    /**
     * Clears pooled objects mapping to given key.
     *
     * @param key                  to locate related pooled objects
     * @param forceRecycleBorrowed is true,close using objects directly;false that pool waiting using objects return to pool and then close it by physically.
     * @throws BeePooledObjectKeyException if key is null or default
     */
    void clearKeyObjects(K key, boolean forceRecycleBorrowed) throws Exception;

    /**
     * Deletes given pooled key from pool.
     *
     * @param key is a key to remove
     * @return true when delete successful,otherwise return false
     * @throws BeePooledObjectKeyException if key is null or default
     */
    boolean deleteKey(K key) throws Exception;

    /**
     * Deletes given pooled key from pool.
     *
     * @param key                  is a key may map to a sub pool
     * @param forceRecycleBorrowed is true,objects in using are closed directly;is false,they are closed when return to pool
     * @return true when delete successful,otherwise return false
     * @throws BeePooledObjectKeyException if key is null or default
     */
    boolean deleteKey(K key, boolean forceRecycleBorrowed) throws Exception;

    //***************************************************************************************************************//
    //                                     4: Pool Log Print(2)                                                      //
    //***************************************************************************************************************//

    /**
     * A switch method to enable or disable logs print of pool and apply change on all pooled category keys.
     *
     * @param enable is true that print, false not print
     */
    void enableLogPrinter(boolean enable) throws Exception;

    /**
     * A switch method to enable or disable logs print flag in target pooled key.
     *
     * @param key    is target pooled key
     * @param enable is true that print, false disable print
     */
    void enableLogPrinter(K key, boolean enable) throws Exception;

    //***************************************************************************************************************//
    //                                     5: Pool Monitoring(2)                                                     //
    //***************************************************************************************************************//

    /**
     * Queries pool whether is closed.
     *
     * @return true if pool closed,otherwise return false
     */
    boolean isClosed();

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeObjectKeyMonitorVo}.
     *
     * @param includeKeys is true,include keys monitor info
     * @return monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo(boolean includeKeys) throws Exception;

    /**
     * Get monitoring of given key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectKeyMonitorVo getKeyMonitorVo(K key) throws Exception;

    //***************************************************************************************************************//
    //                                     6: Pool blocking interrupts(2)                                            //
    //***************************************************************************************************************//

    /**
     * Interrupts waiting threads in pool.
     *
     * @return interrupted threads
     */
    List<Thread> interruptWaitingThreads() throws Exception;

    /**
     * Interrupts waiting threads on given pooled key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return interrupted threads
     * @throws Exception when key is null or not exist key in pool
     */
    List<Thread> interruptWaitingThreads(K key) throws Exception;

    //***************************************************************************************************************//
    //                                     7: Pool method logs(4)                                                    //
    //***************************************************************************************************************//

    /**
     * A switch method to enable or disable pool logs collect
     *
     * @param enable is true that cache collect method logs;false that cache stop works
     */
    void enableLogCache(boolean enable) throws Exception;

    /**
     * Set a new log listener to pool,null listener is acceptable.
     *
     * @param listener to handle method logs
     */
    void changeLogListener(BeeMethodLogListener<K> listener) throws Exception;

    /**
     * Clears logs of pool
     **/
    void clearPoolLogs() throws Exception;

    /**
     * Gets logs of pool
     *
     * @return a result list
     */
    List<BeeMethodLog<K>> getPoolLogs() throws Exception;


    //***************************************************************************************************************//
    //                                     8: Key method logs(6)                                                     //
    //***************************************************************************************************************//

    /**
     * A switch method to enable or disable log collection on key
     *
     * @param enable is true that cache collect method logs;false that cache stop works
     */
    void enableLogCache(K key, boolean enable) throws Exception;

    /**
     * Set a new log listener to pool,null listener is acceptable.
     *
     * @param listener to handle method logs
     */
    void changeLogListener(K key, BeeMethodLogListener<K> listener) throws Exception;

    /**
     * Clears logs of given pooled key
     **/
    void clearKeyLogs(K key) throws Exception;

    /**
     * Gets logs of given pooled key
     *
     * @return a result list
     */
    List<BeeMethodLog<K>> getKeyLogs(K key) throws Exception;

    /**
     * Clears object call logs of given key.
     **/
    void clearKeyObjectLogs(K key) throws Exception;

    /**
     * Gets object call logs of given key.
     *
     * @return a result list
     */
    List<BeeMethodLog<K>> getKeyObjectLogs(K key) throws Exception;
}