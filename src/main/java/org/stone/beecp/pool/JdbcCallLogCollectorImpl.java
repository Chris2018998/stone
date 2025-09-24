/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeJdbcCallLog;
import org.stone.beecp.BeeJdbcCallLogCollector;
import org.stone.beecp.BeeJdbcCallLogListener;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.beecp.BeeJdbcCallLog.Type_Execution_SQL;
import static org.stone.beecp.BeeJdbcCallLog.Type_Get_Connection;

/**
 * Default implementation of {@link BeeJdbcCallLogCollector} interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class JdbcCallLogCollectorImpl implements BeeJdbcCallLogCollector {
    private int cacheSize;
    private long slowGet;
    private long slowExec;
    private BeeJdbcCallLogListener listener;

    private AtomicInteger conLogCount;
    private AtomicInteger sqlLogCount;
    private ConcurrentLinkedQueue<BeeJdbcCallLog> conLogQueue;
    private ConcurrentLinkedQueue<BeeJdbcCallLog> sqlLogQueue;

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
    public void init(int cacheSize, long slowGet, long slowExec, BeeJdbcCallLogListener listener) {
        this.cacheSize = cacheSize;
        this.slowGet = slowGet;
        this.slowExec = slowExec;
        this.listener = listener;

        this.conLogCount = new AtomicInteger(0);
        this.sqlLogCount = new AtomicInteger(0);
        this.conLogQueue = new ConcurrentLinkedQueue<>();
        this.sqlLogQueue = new ConcurrentLinkedQueue<>();
    }

    //***************************************************************************************************************//
    //                                         1: Collector maintain                                                 //
    //***************************************************************************************************************//

    /**
     * clear all logs
     */
    public void clear() {
        this.conLogCount.set(0);
        this.sqlLogCount.set(0);
        this.conLogQueue.clear();
        this.sqlLogQueue.clear();
    }

    /**
     * clean timeout logs
     *
     * @param timeout,if elapsed time is not less than this value,log can be removed from this collector
     */
    public void clear(long timeout) {
        if (timeout > 0L) {
            long curTime = System.currentTimeMillis();
            conLogQueue.removeIf(log -> log.getEndTime() > 0 && curTime - log.getEndTime() >= timeout);
            sqlLogQueue.removeIf(log -> log.getEndTime() > 0 && curTime - log.getEndTime() >= timeout);
        } else {
            int curConLogCount = conLogCount.get();
            do {
                if (conLogCount.compareAndSet(curConLogCount, curConLogCount - 1)) {
                    if (conLogQueue.poll() == null) break;
                    curConLogCount--;
                }
            } while (curConLogCount > 0);

            int cursqlLogCount = sqlLogCount.get();
            do {
                if (sqlLogCount.compareAndSet(cursqlLogCount, cursqlLogCount - 1)) {
                    if (sqlLogQueue.poll() == null) break;
                    cursqlLogCount--;
                }
            } while (cursqlLogCount > 0);
        }
    }

    /**
     * get log with type
     *
     * @param type is log type
     * @return a list of
     */
    public Collection<BeeJdbcCallLog> getLog(int type) {
        if (type == Type_Get_Connection) {
            return new ArrayList<>(conLogQueue);
        } else {
            return new ArrayList<>(sqlLogQueue);
        }
    }

    //***************************************************************************************************************//
    //                                         2: log record                                                         //
    //***************************************************************************************************************//

    /**
     * Start to call a method and a log object is return this start method
     *
     * @param type       is method call type
     * @param method     is method name,for example:getConnection()
     * @param parameters is an array of method parameters
     */
    public BeeJdbcCallLog startCall(int type, String method, Object[] parameters, String preparedSQL) {
        BeeJdbcCallLog log = new BeeJdbcCallLog(type, method, parameters);
        log.setStartTime(System.currentTimeMillis());

        if (type == Type_Get_Connection) {
            conLogQueue.offer(log);
            if (conLogCount.incrementAndGet() > cacheSize) {
                conLogQueue.poll();
                conLogCount.decrementAndGet();
            }
        } else {//Type_Execute_SQL
            if (parameters == null || parameters.length == 0) {
                log.setSql(preparedSQL);
            } else {
                log.setSql((String) parameters[0]);
            }

            sqlLogQueue.offer(log);
            if (sqlLogCount.incrementAndGet() > cacheSize) {
                sqlLogQueue.poll();
                sqlLogCount.decrementAndGet();
            }
        }

        return log;
    }

    /**
     * update result info to log object
     *
     * @param callResult is result of target method call
     * @param log        generated from startCall method
     * @preparedParameters is a parameter array of PreparedSQL or CallableSQL
     */
    public void endCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeJdbcCallLog log) {
        log.setResult(callResult, preparationTookTime, preparedParameters);
        log.setEndTime(System.currentTimeMillis());

        if (listener != null) {
            if ((Type_Get_Connection == log.getType() && slowGet > 0L && log.getEndTime() - log.getStartTime() >= slowGet)
                    || (Type_Execution_SQL == log.getType() && slowExec > 0L && log.getEndTime() - log.getStartTime() >= slowExec)) {
                try {
                    listener.onSlow(log);
                } catch (Throwable e) {
                    //do nothing
                }
            }
        }
    }

    /**
     * update exception to log object
     *
     * @param failCause is result of target method call
     * @param log       generated from startCall method
     * @preparedParameters is a parameter array of PreparedSQL or CallableSQL
     */
    public void endOnException(Throwable failCause, long preparationTookTime, Object[] preparedParameters, BeeJdbcCallLog log) {
        log.setException(failCause, preparationTookTime, preparedParameters);
        log.setEndTime(System.currentTimeMillis());

        if (listener != null) {
            try {
                listener.onException(log);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    /**
     * Cancel statement in executing
     *
     * @param uuid log uuid key
     */
    public void cancelStatement(Object uuid) throws SQLException {
        for (BeeJdbcCallLog log : sqlLogQueue) {
            if (log.getUUID().equals(uuid)) {
                log.cancelStatement();
            }
        }
    }
}
