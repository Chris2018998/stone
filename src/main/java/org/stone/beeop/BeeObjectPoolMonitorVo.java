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
import java.util.Map;

/**
 * A vo interface to represent monitoring info of pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPoolMonitorVo extends Serializable {

    //***************************************************************************************************************//
    //                                     1: Pool configuration                                                     //
    //***************************************************************************************************************//
    String getPoolName();

    //***************************************************************************************************************//
    //                                     2: Pool State`methods                                                     //
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
    //                                     3: Pool Log                                                               //
    //***************************************************************************************************************//
    boolean isEnabledLogPrinter();

    boolean isEnabledMethodLogCache();

    //***************************************************************************************************************//
    //                                     4: key MonitorVo                                                          //
    //***************************************************************************************************************//
    Map<String, BeeObjectKeyMonitorVo> getKeyMonitorVos();

    BeeObjectKeyMonitorVo getKeyMonitorVo(String keyName);

}
