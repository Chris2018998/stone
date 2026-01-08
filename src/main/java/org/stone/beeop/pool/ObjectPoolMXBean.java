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

import org.stone.beeop.BeeObjectKeyMonitorVo;
import org.stone.beeop.BeeObjectPoolMonitorVo;

/**
 * Pool JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ObjectPoolMXBean<K> {

    void enableLogPrint(boolean enable);

    BeeObjectPoolMonitorVo<K> getPoolMonitorVo(boolean keyMonitor);

    BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) throws Exception;
}