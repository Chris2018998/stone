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
 * A vo interface to represent monitoring info of a pooled key.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectKeyMonitorVo extends Serializable {

    //***************************************************************************************************************//
    //                                     1: Pool Key name                                                          //
    //***************************************************************************************************************//
    String getKeyName();

    //***************************************************************************************************************//
    //                                     2: Pool State`methods                                                      //
    //***************************************************************************************************************//
    boolean isNew();

    boolean isReady();

    boolean isClosing();

    boolean isClosed();

    boolean isStarting();

    boolean isRestarting();

    boolean isRestartFailed();

    boolean isSuspended();

    //***************************************************************************************************************//
    //                                     3: Size of objects(pooled objects, semaphore,waiter)                       //
    //***************************************************************************************************************//
    //return size of idle objects related of pooled key
    int getIdleSize();

    //return size of borrowed object of pooled key
    int getBorrowedSize();

    //return size of objects in creating of pooled key
    int getCreatingSize();

    //return count of objects creation timeout of pooled key
    int getCreatingTimeoutSize();

    //return remain size of semaphore permits of pooled key
    int getSemaphoreRemainSize();

    //return size of waiters on semaphore of pooled key
    int getSemaphoreWaitingSize();

    //return size of waiters in wait queue of pooled key
    int getTransferWaitingSize();

    //***************************************************************************************************************//
    //                                     4: Other                                                                   //
    //***************************************************************************************************************//
    //Query log print is whether enabled on pooled key
    boolean isEnabledLogPrinter();

    //Query log cache is whether enabled on pooled key
    boolean isEnabledLogCache();
}
