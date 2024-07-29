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
 * A container interface on maintaining keyed pooled objects which can be borrowed out.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool {

    //***************************************************************************************************************//
    //                                             1: Pool initialization                                            //
    //***************************************************************************************************************//

    /**
     * Pool initializes with a configuration object
     *
     * @param config is a configuration object for pool initialization
     * @throws Exception while configuration checks failed or pool initializes failed
     */
    void init(BeeObjectSourceConfig config) throws Exception;

    //***************************************************************************************************************//
    //                                             2: Maintenance by keys                                            //
    //***************************************************************************************************************//

    /**
     * get keys maintained in pool
     *
     * @return a keys array
     */
    Object[] keys();

    /**
     * get default key of pooled objects
     *
     * @return default key in pool
     */
    Object getDefaultKey();

    /**
     * Attempts to get an object from pool with default key,return a handle of a pooled object when success
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
     * @return handle of a borrowed object
     * @throws ObjectCreateException         when failed to create a new object
     * @throws ObjectGetTimeoutException     when timeout on wait
     * @throws ObjectGetInterruptedException when interruption on wait
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    /**
     * Deletes a pooled key from pool and remove all pooled objects associated with the key
     *
     * @param key is a key to remove
     * @throws ObjectKeyException if key is null
     * @throws ObjectKeyException if key is default key
     */
    void deleteKey(Object key) throws Exception;

    /**
     * Deletes a pooled key from pool and remove all pooled objects associated with the key
     *
     * @param key             is a key to remove from pool
     * @param forceCloseUsing is true,objects in using are closed directly;is false,they are closed when return to pool
     * @throws ObjectKeyException if key is null
     * @throws ObjectKeyException if key is default key
     */
    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

    /**
     * Delete all objects from a group with key.
     *
     * @param key             can being map to an object group
     * @param forceCloseUsing is true,direct closes borrowed objects and removes them from key mapping group;false,closes borrowed objects on they return to pool
     * @throws Exception when objects deleted failed
     */
    void deleteObjects(Object key, boolean forceCloseUsing) throws Exception;

    /**
     * Get start time in creating an object in pool,timeunit:nanoseconds
     *
     * @param key can being map to an object group
     * @return hold time on pool lock
     */
    long getCreatingTime(Object key);

    /**
     * checks current creation timeout,refer to {@link #getCreatingTime(Object key)}
     *
     * @return an indicator of pool lock hold timeout,true or false
     */
    boolean isCreatingTimeout(Object key);

    /**
     * interrupt creation thread of connection and all waiters to create connections
     *
     * @return interrupted threads
     */
    Thread[] interruptOnCreation(Object key);

    /**
     * Gets monitor info of a group by key.
     *
     * @param key may be mapping to a set of pooled objects
     * @return monitor of an object group
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getMonitorVo(Object key) throws Exception;

    /**
     * A switch method on runtime logs print by key
     *
     * @param key       pooled key
     * @param indicator is value,print logs;false,not print
     * @throws Exception when key is null or not exist key in pool
     */
    void setPrintRuntimeLog(Object key, boolean indicator) throws Exception;

    //***************************************************************************************************************//
    //                                             3: other methods of pool                                          //
    //***************************************************************************************************************//

    /**
     * This invocation cause pool to stop work,closes all objects and removes them,pool state marked as closed value
     * when completion and all operations on pool are disabled.
     */
    void close();

    /**
     * Gets pool status whether in closed.
     *
     * @return a boolean value of pool close status
     */
    boolean isClosed();

    /**
     * A switch method on runtime logs print on all groups
     *
     * @param indicator is true,pool prints runtime logs；false,not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Returns a view object contains pool monitor info,such as state,idle,using and so on.
     *
     * @return monitor vo
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Closes all objects and removes them from all groups.
     *
     * @param forceCloseUsing is true,direct closes all borrowed objects and removes them;false,closes borrowed objects on they return to pool
     */
    void clear(boolean forceCloseUsing) throws Exception;

    /**
     * Closes all objects and removes them from pool,then try to do reinitialization on pool with a new configuration object.
     *
     * @param forceCloseUsing is true,direct closes all borrowed objects and removes them;false,closes borrowed objects on they return to pool
     * @param config          is a configuration object for pool reinitialize
     * @throws BeeObjectSourceConfigException when config is null
     * @throws PoolInitializeFailedException  when pool reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;

}