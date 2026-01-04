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

import java.util.List;

/**
 * Methods execution listener interface.
 *
 * @author Chris Liao
 */
public interface BeeMethodExecutionListener<K> {

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     */
    void onMethodStart(BeeMethodExecutionLog<K> log) throws Exception;

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     */
    void onMethodEnd(BeeMethodExecutionLog<K> log) throws Exception;

    /**
     * Handle a list of long-running logs
     *
     * @param logList to be handled
     */
    List<Boolean> onLongRunningDetected(List<BeeMethodExecutionLog<K>> logList);

}
