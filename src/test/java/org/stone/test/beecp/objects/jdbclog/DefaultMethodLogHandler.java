/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.objects.jdbclog;

import org.stone.beecp.BeeMethodExecutionLog;
import org.stone.beecp.BeeMethodExecutionListener;

import java.util.List;

/**
 * Default implementation of {@link BeeMethodExecutionListener} interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class DefaultMethodLogHandler implements BeeMethodExecutionListener {

    public void onMethodStart(BeeMethodExecutionLog log) {

    }

    public void onMethodEnd(BeeMethodExecutionLog log) {

    }

    public void onLongRunningDetected(List<BeeMethodExecutionLog> slowList) {

    }
}
