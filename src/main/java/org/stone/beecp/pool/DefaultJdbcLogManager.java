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
import org.stone.beecp.BeeJdbcCallLogHandler;
import org.stone.beecp.BeeJdbcCallLogManager;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beecp.BeeJdbcCallLog.Type_Execution_SQL;
import static org.stone.beecp.BeeJdbcCallLog.Type_Get_Connection;

/**
 * Default implementation of {@link BeeJdbcCallLogManager} interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class DefaultJdbcLogManager implements BeeJdbcCallLogManager {
    private boolean syncMode;
    //log handler
    private BeeJdbcCallLogHandler handler;
    //slow threshold value of connection get,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowConnectionGetThreshold}
    private long slowConnectionGetThreshold;
    //slow threshold of sql execution,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowSQLExecutionThreshold}
    private long slowSQLExecutionThreshold;

    //logs queue of connection get
    private LinkedBlockingQueue<DefaultJdbcCallLog> conLogQueue;
    //logs queue of sql execution
    private LinkedBlockingQueue<DefaultJdbcCallLog> sqlLogQueue;

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log collector.
     *
     * @param cacheSize is capacity of logs cache
     * @param slowGet   is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec  is slow threshold of sql execution,time unit:milliseconds
     * @param handler   is a log handler
     */
    public void init(int cacheSize,
                     long slowGet, long slowExec,
                     boolean syncMode, BeeJdbcCallLogHandler handler) {


        this.handler = handler;
        this.syncMode = syncMode;
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
        List<DefaultJdbcCallLog> conPendingRemovalLogList = new LinkedList<>();
        List<DefaultJdbcCallLog> sqlPendingRemovalLogList = new LinkedList<>();

        long currentTime = System.currentTimeMillis();
        if (syncMode) {
            //timeout check on connection logs
            for (DefaultJdbcCallLog log : conLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    conPendingRemovalLogList.add(log);
                }
            }

            //timeout check on sql execution logs
            for (DefaultJdbcCallLog log : sqlLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    sqlPendingRemovalLogList.add(log);
                }
            }
        } else {//async mode
            processLogList = new ArrayList<>(10);
            for (DefaultJdbcCallLog log : conLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    conPendingRemovalLogList.add(log);
                }
                if (!log.isHandled() && log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold) {
                    log.setSlow(true);
                    log.setHandled(true);
                    processLogList.add(log);
                }
            }

            //timeout check on sql execution logs
            for (DefaultJdbcCallLog log : sqlLogQueue) {
                if (currentTime - log.getEndTime() >= timeout) {
                    log.setRemoved(true);
                    sqlPendingRemovalLogList.add(log);
                }

                if (!log.isHandled() && log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold) {
                    log.setSlow(true);
                    log.setHandled(true);
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
                boolean[] flags = this.handler.handle(processLogList);
                for (int i = 0, l = flags.length; i < l; i++) {
                    ((DefaultJdbcCallLog) (processLogList.get(i))).setHandled(flags[i]);
                }
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
    public BeeJdbcCallLog startCall(int type, String method, Object[] parameters, String sql, Statement statement) {
        DefaultJdbcCallLog log = new DefaultJdbcCallLog(type, method, parameters);
        log.setStartTime(System.currentTimeMillis());
        log.setStatement(statement);
        offerQueue(log, type, parameters, sql);
        return log;
    }

    private void offerQueue(DefaultJdbcCallLog log, int type, Object[] parameters, String sql) {
        if (type == Type_Get_Connection) {
            for (; ; ) {
                if (conLogQueue.offer(log)) {
                    break;
                } else {
                    DefaultJdbcCallLog other = conLogQueue.poll();
                    if (other != null) other.setRemoved(true);
                }
            }
        } else {//Type_Execute_SQL
            if (parameters == null || parameters.length == 0) {
                log.setSql(sql);
            } else {
                log.setSql((String) parameters[0]);
            }
            for (; ; ) {
                if (sqlLogQueue.offer(log)) {
                    break;
                } else {
                    DefaultJdbcCallLog other = sqlLogQueue.poll();
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
        DefaultJdbcCallLog defaultTypeLog = (DefaultJdbcCallLog) log;
        defaultTypeLog.setResult(callResult, preparationTookTime, preparedParameters);
        defaultTypeLog.setEndTime(System.currentTimeMillis());

        if (log.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, log.getType(), log.getParameters(), log.getSql());
        }

        if (this.syncMode && ((Type_Get_Connection == log.getType() && log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold)
                || (Type_Execution_SQL == log.getType() && log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold))) {
            try {
                defaultTypeLog.setSlow(true);
                defaultTypeLog.setHandled(handler.handle(log));
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
        DefaultJdbcCallLog defaultTypeLog = (DefaultJdbcCallLog) log;

        defaultTypeLog.setException(failCause, preparationTookTime, preparedParameters);
        defaultTypeLog.setEndTime(System.currentTimeMillis());

        if (log.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, log.getType(), log.getParameters(), log.getSql());
        }

        if (this.syncMode) {
            try {
                defaultTypeLog.setHandled(handler.handle(log));
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
    public void cancelRunningStatement(Object uuid) throws SQLException {
        for (BeeJdbcCallLog log : sqlLogQueue) {
            if (log.getId().equals(uuid)) {
                log.cancelRunningStatement();
            }
        }
    }
}
