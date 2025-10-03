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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beecp.BeeJdbcCallLog.Type_Execution_SQL;
import static org.stone.beecp.BeeJdbcCallLog.Type_Get_Connection;

/**
 * Default implementation of {@link BeeJdbcCallLogCollector} interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class JdbcCallLogCollectorImpl implements BeeJdbcCallLogCollector {
    private boolean listenInSync;
    //log listener
    private BeeJdbcCallLogListener listener;
    //slow threshold value of connection get,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowConnectionGetThreshold}
    private long slowConnectionGetThreshold;
    //slow threshold of sql execution,time unit:milliseconds,,refer to {@code BeeDataSourceConfig.slowSQLExecutionThreshold}
    private long slowSQLExecutionThreshold;

    //logs queue of connection get
    private LinkedBlockingQueue<BeeJdbcCallLog> conLogQueue;
    //logs queue of sql execution
    private LinkedBlockingQueue<BeeJdbcCallLog> sqlLogQueue;

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
    public void init(int cacheSize,
                     long slowGet, long slowExec,
                     boolean listenInSync, BeeJdbcCallLogListener listener) {


        this.listener = listener;
        this.listenInSync = listenInSync;
        this.slowConnectionGetThreshold = slowGet;
        this.slowSQLExecutionThreshold = slowExec;

        this.conLogQueue = new LinkedBlockingQueue<>(cacheSize);
        this.sqlLogQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    //***************************************************************************************************************//
    //                                         1: Collector maintain                                                 //
    //***************************************************************************************************************//

    /**
     * Clear timeout logs from collector.
     *
     * @param timeout to check timeout logs
     */
    public void clear(long timeout) {
        if (timeout <= 0L) {
            this.conLogQueue.clear();
            this.sqlLogQueue.clear();
            return;
        }

        List<BeeJdbcCallLog> processLogList = null;
        List<BeeJdbcCallLog> conPendingRemovalLogList = new LinkedList<>();
        List<BeeJdbcCallLog> sqlPendingRemovalLogList = new LinkedList<>();

        long currentTime = System.currentTimeMillis();
        if (listenInSync) {
            //timeout check on connection logs
            for (BeeJdbcCallLog log : conLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    conPendingRemovalLogList.add(log);
                }
            }

            //timeout check on sql execution logs
            for (BeeJdbcCallLog log : sqlLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    sqlPendingRemovalLogList.add(log);
                }
            }
        } else {//async mode
            processLogList = new LinkedList<>();
            for (BeeJdbcCallLog log : conLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    conPendingRemovalLogList.add(log);
                }
                if (!log.isProcessed() && log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold) {
                    log.setProcessed(true);
                    processLogList.add(log);
                }
            }

            //timeout check on sql execution logs
            for (BeeJdbcCallLog log : sqlLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    sqlPendingRemovalLogList.add(log);
                }

                if (!log.isProcessed() && log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold) {
                    log.setProcessed(true);
                    processLogList.add(log);
                }
            }
        }

        if (!conPendingRemovalLogList.isEmpty())
            conLogQueue.removeAll(conPendingRemovalLogList);
        if (!sqlPendingRemovalLogList.isEmpty())
            sqlLogQueue.removeAll(sqlPendingRemovalLogList);

        if (processLogList != null && !processLogList.isEmpty()) {
            try {
                this.listener.process(processLogList);
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    public List<BeeJdbcCallLog> getLog(int type) {
        if (type == Type_Get_Connection) {
            return new ArrayList<>(conLogQueue);
        } else {
            return new ArrayList<>(sqlLogQueue);
        }
    }

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
        offerQueue(log, type, parameters, preparedSQL);
        return log;
    }

    private void offerQueue(BeeJdbcCallLog log, int type, Object[] parameters, String preparedSQL) {
        if (type == Type_Get_Connection) {
            for (; ; ) {
                if (conLogQueue.offer(log)) {
                    break;
                } else {
                    BeeJdbcCallLog other = conLogQueue.poll();
                    if (other != null) other.setRemoved(true);
                }
            }
        } else {//Type_Execute_SQL
            if (parameters == null || parameters.length == 0) {
                log.setSql(preparedSQL);
            } else {
                log.setSql((String) parameters[0]);
            }
            for (; ; ) {
                if (sqlLogQueue.offer(log)) {
                    break;
                } else {
                    BeeJdbcCallLog other = sqlLogQueue.poll();
                    if (other != null) other.setRemoved(true);
                }
            }
        }
    }

    //***************************************************************************************************************//
    //                                         2: log record                                                         //
    //***************************************************************************************************************//

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

        if (log.isRemoved()) {
            log.setRemoved(false);
            offerQueue(log, log.getType(), log.getParameters(), log.getSql());
        }

        if (this.listenInSync && ((Type_Get_Connection == log.getType() && slowConnectionGetThreshold > 0L && log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold)
                || (Type_Execution_SQL == log.getType() && slowSQLExecutionThreshold > 0L && log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold))) {
            try {
                listener.process(log);
                log.setProcessed(true);
            } catch (Throwable e) {
                //do nothing
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

        if (log.isRemoved()) {
            log.setRemoved(false);
            offerQueue(log, log.getType(), log.getParameters(), log.getSql());
        }

        if (this.listenInSync) {
            try {
                listener.process(log);
                log.setProcessed(true);
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
    public void cancelSqlExecuting(Object uuid) throws SQLException {
        for (BeeJdbcCallLog log : sqlLogQueue) {
            if (log.getId().equals(uuid)) {
                log.cancelSqlExecuting();
            }
        }
    }
}
