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
    //                                             2:Maintenance by object key                                       //
    //***************************************************************************************************************//

    /**
     * returns category keys in pool,if no keys,then return a empty array
     *
     * @return pooled keys exists in pool
     */
    Object[] keys();

    /**
     * returns default category keys
     *
     * @return pooled keys exists in pool
     */
    Object getDefaultKey();

    /**
     * borrows a default keyed object and returns a resulted handle
     *
     * @return handle of a pooled object
     * @throws Exception when get failed
     */
    BeeObjectHandle getObjectHandle() throws Exception;

    /**
     * borrows a keyed object and returns a resulted handle
     *
     * @return handle of a pooled object
     * @throws Exception when gets failed
     */
    BeeObjectHandle getObjectHandle(Object key) throws Exception;

    /**
     * deletes keyed objects from pool
     *
     * @param key a deleted object key
     * @throws Exception when delete failed
     */
    void deleteKey(Object key) throws Exception;

    /**
     * deletes keyed objects from pool
     *
     * @param key             object key
     * @param forceCloseUsing is true,then remove directly using objects,otherwise delay to remove using until they returned
     * @throws Exception when delete failed
     */
    void deleteKey(Object key, boolean forceCloseUsing) throws Exception;

    BeeObjectPoolMonitorVo getPoolMonitorVo(Object key) throws Exception;

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