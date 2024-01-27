/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop;

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
     * returns pooled keys,if not exists keys,then return an empty array
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
     * Borrows a pooed object by default keyed
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
    void close();

    boolean isClosed();

    void setPrintRuntimeLog(boolean enable);

    BeeObjectPoolMonitorVo getPoolMonitorVo();

    void clear(boolean forceCloseUsing) throws Exception;

    void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception;

}