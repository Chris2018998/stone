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

import java.sql.SQLException;

/**
 * keyed object pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeKeyedObjectPool {

    //***************************************************************************************************************//
    //                                             1:Pool initialization                                             //
    //***************************************************************************************************************//

    /**
     * Launch pool with initial configuration which contains some field level items.
     *
     * @param config is a configuration object for pool initialization
     * @throws Exception when configuration checks failed or pool initializes failed
     */
    void init(BeeObjectSourceConfig config) throws Exception;


    //***************************************************************************************************************//
    //                                             2:Maintenance by keys                                             //
    //***************************************************************************************************************//

    /**
     * returns pooled keys include default key
     *
     * @return pooled keys exists in pool
     */
    Object[] keys();

    /**
     * returns default pooled key
     *
     * @return pooled keys exists in pool
     */
    Object getDefaultKey();

    /**
     * Borrows a pooed object by default key
     *
     * @return handle of a pooled object
     * @throws Exception when get failed
     */
    BeeObjectHandle getObjectHandle() throws Exception;

    /**
     * Borrows a pooled object with parameter key
     *
     * @return handle of a pooled object
     * @throws Exception when gets failed
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    /**
     * deletes key and key related pooled objects from pool
     *
     * @param key may be mapping to a set of pooled objects
     * @throws Exception when delete failed
     */
    void deleteKey(Object key) throws Exception;

    /**
     * deletes key and key related pooled objects from pool
     *
     * @param key             may be mapping to a set of pooled objects
     * @param forceCloseUsing is true,then remove directly using objects,otherwise delay to remove using until they returned
     * @throws Exception when delete failed
     */
    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

    /**
     * only deletes key related pooled objects from pool
     *
     * @param key             may be mapping to a set of pooled objects
     * @param forceCloseUsing is true,then remove directly using objects,otherwise delay to remove using until they returned
     * @throws Exception when delete failed
     */
    void deleteObjects(Object key, boolean forceCloseUsing) throws Exception;

    /**
     * get elapsed time of lock owner thread
     */
    long getElapsedTimeSinceCreationLock(Object key);

    /**
     * interrupt queued waiters on creation lock and acquired thread,which may be stuck in driver
     */
    void interruptThreadsOnCreationLock(Object key);

    /**
     * get monitor info by key
     *
     * @param key may be mapping to a set of pooled objects
     * @return a monitor object by key
     * @throws Exception when key is null or not exist key in pool
     */
    BeeObjectPoolMonitorVo getKeyMonitorVo(Object key) throws Exception;

    /**
     * enable indicator on runtime print log
     *
     * @param key       pooled key
     * @param indicator is a boolean value,true on,false off
     * @throws Exception when key is null or not exist key in pool
     */
    void setPrintRuntimeLog(Object key, boolean indicator) throws Exception;

    //***************************************************************************************************************//
    //                                             3: other methods of pool                                          //
    //***************************************************************************************************************//

    /**
     * Method invocation to shut down pool,there is a thead-safe control,only one thread success call
     * it at concurrent,the pool state mark to be closed and rejects coming requests with a exception.
     * This method support duplicated call when in closed state,do nothing,just return.
     */
    void close();

    /**
     * test pool whether in closed state
     *
     * @return a boolean,true closed
     */
    boolean isClosed();

    /**
     * Changes on indicator of pool runtime log print
     *
     * @param indicator true,print;false,not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Returns a view object contains pool monitor info,such as state,idle,using and so on.
     *
     * @return monitor vo
     */
    BeeObjectPoolMonitorVo getPoolMonitorVo();

    /**
     * Removes all pooled objects,this method should work under synchronization control,success caller update
     * pool state from {@code ObjectPoolStatics.POOL_READY} to {@code ObjectPoolStatics.POOL_CLEARING} and
     * interrupts all blocking waiters to leave from pool.Before completion of clearing,the pool rejects borrowing
     * requests.All idle objects closed immediately and removed,if parameter<h1>forceCloseUsing</h1>is true,
     * do same operation on all using objects like idle,but false,the caller thead waits using return to pool,
     * then close them.Finally,pool state reset to {@code ObjectPoolStatics.POOL_READY} when done.
     *
     * @param forceCloseUsing is a indicator that direct close or delay close on using objects
     */
    void clear(boolean forceCloseUsing) throws Exception;

    /**
     * Removes all pooled objects and restarts pool with new configuration when the config parameter is not null,
     * the method is similar to the previous{@link #clear}.
     *
     * @param forceCloseUsing is a indicator that direct close or delay close on using objects
     * @param config          is new configuration for pool restarting
     * @throws SQLException when configuration checks failed or pool re-initializes failed
     */
    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;

}