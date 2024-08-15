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
     * Closes pool
     */
    void close();

    /**
     * Query pool state whether is closed.
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    /**
     * Turns on switch of print runtime logs of pool,or turns off
     *
     * @param indicator is true that print logs,false not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Gets runtime monitor object of pool.
     *
     * @return monitor of pool
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Removes all pooled objects.
     *
     * @param forceCloseUsing is true that close borrowed objects immediately;false that close borrowed objects when them return to pool
     */
    void clear(boolean forceCloseUsing) throws Exception;

    /**
     * Removes all pooled objects,and re-initializes pool with a new configuration object.
     *
     * @param forceCloseUsing is true that close borrowed objects immediately;false that close borrowed objects when them return to pool
     * @param config          is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInitializeFailedException  when reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;


    //***************************************************************************************************************//
    //                                    Methods to borrow pooled objects                                           //
    //***************************************************************************************************************//

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
     * @param key may be mapping to a set of pooled objects
     * @return handle of a borrowed object
     * @throws ObjectCreateException         when failed to create a new object
     * @throws ObjectGetTimeoutException     when timeout on wait
     * @throws ObjectGetInterruptedException when interruption on wait
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;


    //***************************************************************************************************************//
    //                                  Methods to monitor runtime pool                                              //
    //***************************************************************************************************************//

    /**
     * Gets start time of creating an object for a thread,timeunit:nanoseconds
     *
     * @param key may be mapping to a set of pooled objects
     * @return start time of creation
     */
    long getCreatingTime(Object key) throws Exception;

    /**
     * Query elapsed time of current creation is whether timeout,call {@link #getCreatingTime(Object)} to get start time.
     *
     * @return true that already timeout,false that not timeout or no creation
     */
    boolean isCreatingTimeout(Object key) throws Exception;

    /**
     * attempt to interrupt a thread in creating object and waiting to create objects
     *
     * @return interrupted threads
     */
    Thread[] interruptOnCreation(Object key) throws Exception;

    /**
     * operation on log switch to disable log print or enable print
     *
     * @param key       pooled key
     * @param indicator is true,print logs;false,not print
     * @throws Exception when key is null or not exist key in pool
     */
    void setPrintRuntimeLog(Object key, boolean indicator) throws Exception;

    /**
     * Get monitor object of sub pool by key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception;


    //***************************************************************************************************************//
    //                                     Methods to maintain pooled keys                                           //
    //***************************************************************************************************************//

    /**
     * gets keys maintained in pool
     *
     * @return a keys array
     */
    Object[] keys();

    /**
     * Deletes sub pool map to a given parameter key.
     *
     * @param key is a key to remove
     * @throws ObjectKeyException if key is null
     * @throws ObjectKeyException if key is default key
     */
    void deleteKey(Object key) throws Exception;

    /**
     * Deletes sub pool map to a given parameter key.
     *
     * @param key             is a key may map to a sub pool
     * @param forceCloseUsing is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null
     * @throws ObjectKeyException if key is default key
     */
    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

}