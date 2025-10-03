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

import org.stone.beecp.BeeJdbcCallLogListener;

import java.util.Collection;

/**
 * An interface to collect logs of connection get and logs of SQL execution.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectCallLogCollector<K, V> {

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log collector.
     *
     * @param cacheSize is capacity of logs cache
     * @param slowGet   is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec  is slow threshold of sql execution,time unit:milliseconds
     * @param listener  is a log listener
     */
    void init(int cacheSize, long slowGet, long slowExec, boolean listenInSync, BeeJdbcCallLogListener listener);

    //***************************************************************************************************************//
    //                                         2: logs maintain                                                      //
    //***************************************************************************************************************//

    /**
     * Clear all logs.
     */
    void clear();

    /**
     * Clear timeout logs.
     *
     * @Param timeout is zero,then clear all logs;otherwise only clear timeout logs.
     */
    void clear(long timeout);

    /**
     * Query and get logs with given type.
     *
     * @param type is log type
     * @return a list of logs
     */
    Collection<BeeObjectCallLog<K, V>> getLog(int type);

    //***************************************************************************************************************//
    //                                         3: logs record                                                        //
    //***************************************************************************************************************//

    /**
     * Plugin method is executed at front of proxy methods to generate a start log object.
     *
     * @param key        is a pooled key
     * @param type       is method call type
     * @param method     is method name,for example:getConnection()
     * @param parameters is an array of method parameters
     */
    BeeObjectCallLog<K, V> startCall(K key, int type, String method, Object[] parameters);

    /**
     * Plugin method is executed at end of proxy methods to update result and attach to a log object.
     *
     * @param callResult is result of target method call
     * @param log        generated from {@link #startCall}method
     */
    void endCall(Object callResult, BeeObjectCallLog<K, V> log);

    /**
     * Plugin method is executed at exception catch block of proxy methods to update exception and attach to a log object.
     *
     * @param failCause is result of target method call
     * @param log       generated from startCall method
     */
    void endOnException(Throwable failCause, BeeObjectCallLog<K, V> log);

}
