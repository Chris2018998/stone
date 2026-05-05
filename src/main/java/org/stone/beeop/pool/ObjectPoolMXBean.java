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

import org.stone.beeop.BeeObjectBucketMonitorVo;
import org.stone.beeop.BeeObjectPoolMonitorVo;

/**
 * Pool JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ObjectPoolMXBean {

    void enableLogPrinter(boolean enable) throws Exception;

    void enablePoolLogCache(boolean enable) throws Exception;

    BeeObjectPoolMonitorVo getPoolMonitorVo() throws Exception;


    String[] getBucketKeyNames() throws Exception;

    void enableBucketLogPrinterByName(String keyName, boolean enable) throws Exception;

    void enableBucketLogCacheByName(String keyName, boolean enable) throws Exception;

    BeeObjectBucketMonitorVo getBucketMonitorVoByName(String keyName) throws Exception;
}
