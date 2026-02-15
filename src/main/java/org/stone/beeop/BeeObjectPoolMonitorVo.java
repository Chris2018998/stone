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

import java.io.Serializable;

/**
 * A vo interface to represent monitoring info of pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPoolMonitorVo<K> extends Serializable {

    //***************************************************************************************************************//
    //                                     1: Pool base                                                              //
    //***************************************************************************************************************//
    //return key pool name
    String getPoolName();

    //Query pool is whether fair mode
    boolean isFairMode();

    //return capacity size of keys in pool
    int getMaxKeySize();

    //***************************************************************************************************************//
    //                                     2: Configuration on keys                                                  //
    //***************************************************************************************************************//
    //return max capacity size of key
    int getMaxActiveSizeOfKey();

    //return permit size of semaphore of key
    int getSemaphoreSizeOfKey();

    //Query pool is using ThreadLocal
    boolean useThreadLocalOfKey();

    //***************************************************************************************************************//
    //                                     3: Pool State`methods                                                     //
    //***************************************************************************************************************//
    boolean isLazy();

    boolean isNew();

    boolean isClosing();

    boolean isReady();

    boolean isStarting();

    boolean isRestarting();

    boolean isRestartFailed();

    boolean isSuspended();

    //***************************************************************************************************************//
    //                                     4: Pool other                                                             //
    //***************************************************************************************************************//
    boolean isEnabledLogPrinter();

    boolean isEnabledLogCache();

    BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key);

    BeeObjectKeyMonitorVo<K>[] getKeyMonitorVos();
}
