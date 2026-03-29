/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import java.util.List;

/**
 * Pool JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ObjectPoolMXBean {

    void enableLogPrinter(boolean enable) throws Exception;

    void enableLogCache(boolean enable) throws Exception;

    ObjectPoolMonitorVo getPoolMonitorVo() throws Exception;


    List<String> getKeyNames() throws Exception;

    void enableKeyLogPrinterByName(String keyName, boolean enable) throws Exception;

    void enableKeyLogCacheByName(String keyName, boolean enable) throws Exception;

    ObjectKeyMonitorVo getKeyMonitorVoByName(String keyName) throws Exception;
}
