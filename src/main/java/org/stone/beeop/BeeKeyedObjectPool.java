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
 * Bee Object Pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool {

    /**
     * Pool initializes with a configuration object
     *
     * @param config is a configuration object for pool initialization
     * @throws Exception while configuration checks failed or pool initializes failed
     */
    void init(BeeObjectSourceConfig config) throws Exception;

    /**
     * close pool
     */
    void close();

    /**
     * Checks pool state whether in closed
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    /**
     * Changes a switch to print runtime logs in pool,or not print
     *
     * @param indicator is true,pool prints runtime logs；false,not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Get a monitor object,this object contains some runtime info of pool
     *
     * @return a monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Cleanup pool and all pooled objects removed from pool
     *
     * @param forceCloseUsing is true that close borrowed object immediately;false that close borrowed objects when them return to pool
     */
    void clear(boolean forceCloseUsing) throws Exception;

    /**
     * After cleaned up,pool re-initialize with a new configuration object
     *
     * @param forceCloseUsing is true that close borrowed object immediately;false that close borrowed objects when them return to pool
     * @param config          is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInitializeFailedException  when pool reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;


    //***************************************************************************************************************//
    //                                     some methods with poole key                                               //
    //***************************************************************************************************************//

    /**
     * gets keys maintained in pool
     *
     * @return a keys array
     */
    Object[] keys();

    /**
     * Attempts to get an object from pool
     *
     * @return handle of borrowed object
     * @throws ObjectCreateException         when fail to create a pooled object
     * @throws ObjectGetTimeoutException     when wait timeout for an object released from other borrower
     * @throws ObjectGetInterruptedException that an interruption occurs while borrower waits for a released object
     */
    BeeObjectHandle getObjectHandle() throws Exception;

    /**
     * Attempts to get an object from pool with a specified key
     *
     * @param key is a object
     * @return handle of a borrowed object
     * @throws ObjectCreateException         when failed to create a new object
     * @throws ObjectGetTimeoutException     when timeout on wait
     * @throws ObjectGetInterruptedException when interruption on wait
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    /**
     * Gets start time of creating an object for a thread
     *
     * @param key is
     * @return start time of creation
     */
    long getCreatingTime(Object key);

    /**
     * Gets timeout check result of creating an object for a thread
     *
     * @return true that already timeout,false that not timeout or not creation
     */
    boolean isCreatingTimeout(Object key);

    /**
     * attempt to interrupt thread in creating object and waiting to create objects
     *
     * @return interrupted threads
     */
    Thread[] interruptOnCreation(Object key);

    /**
     * enable indicator to print runtime logs for a pooled key,or not print
     *
     * @param key       pooled key
     * @param indicator is true,print logs;false,not print
     * @throws Exception when key is null or not exist key in pool
     */
    void setPrintRuntimeLog(Object key, boolean indicator) throws Exception;

    /**
     * Get a monitor object by pooled key
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception;

    /**
     * Deletes a set of pooled objects related to the given key
     *
     * @param key is a key to remove
     * @throws ObjectKeyException if key is null
     * @throws ObjectKeyException if key is default key
     */
    void deleteKey(Object key) throws Exception;

    /**
     * Deletes a set of pooled objects related to the given key
     *
     * @param key             is a key to remove from pool
     * @param forceCloseUsing is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null
     * @throws ObjectKeyException if key is default key
     */
    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

}