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

import org.stone.beecp.BeeMethodLog;
import org.stone.beecp.BeeMethodLogHandler;

import java.util.List;

/**
 * Default implementation of {@link BeeMethodLogHandler} interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class DefaultMethodLogHandler implements BeeMethodLogHandler {

    public void handleStartLog(BeeMethodLog log) {

    }

    public void handleEndLog(BeeMethodLog log) {

    }

    public void handleLongRunningLogs(List<BeeMethodLog> slowList) {

    }
}
