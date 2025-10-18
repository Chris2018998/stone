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

import org.stone.beecp.BeeJdbcEventLog;
import org.stone.beecp.BeeJdbcEventLogHandler;
import org.stone.beecp.BeeJdbcEventLogManager;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beecp.BeeJdbcEventLog.Type_Connection_Get;
import static org.stone.beecp.BeeJdbcEventLog.Type_SQL_Execution;

/**
 * Default implementation of {@link BeeJdbcEventLogManager} interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class DefaultJdbcEventLogManager implements BeeJdbcEventLogManager {
    private boolean handleBySyncMode;
    private boolean handleByAsyncMode;
    private BeeJdbcEventLogHandler handler;

    //slow threshold value of connection get,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowConnectionGetThreshold}
    private long slowConnectionGetThreshold;
    //slow threshold of sql execution,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowSQLExecutionThreshold}
    private long slowSQLExecutionThreshold;

    private int maxSize;
    //logs queue of connection get
    private LinkedBlockingQueue<DefaultJdbcEventLog> conLogQueue;
    //logs queue of sql execution
    private LinkedBlockingQueue<DefaultJdbcEventLog> sqlLogQueue;

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log manager.
     *
     * @param cacheSize is capacity of logs cache
     * @param slowGet   is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec  is slow threshold of sql execution,time unit:milliseconds
     * @param handler   is a log handler
     */
    public void init(int cacheSize,
                     long slowGet, long slowExec,
                     boolean syncMode, BeeJdbcEventLogHandler handler) {

        if (handler != null) {
            this.handler = handler;
            this.handleBySyncMode = syncMode;
            this.handleByAsyncMode = !syncMode;
        }

        this.slowConnectionGetThreshold = slowGet;
        this.slowSQLExecutionThreshold = slowExec;

        this.maxSize = cacheSize;
        this.conLogQueue = new LinkedBlockingQueue<>(cacheSize);
        this.sqlLogQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    //***************************************************************************************************************//
    //                                         1: Logs maintain                                                      //
    //***************************************************************************************************************//
    public List<BeeJdbcEventLog> getLog(int type) {
        List<BeeJdbcEventLog> logList = new LinkedList<>();
        switch (type) {
            case Type_Connection_Get: {
                logList.addAll(this.conLogQueue);
                break;
            }
            case Type_SQL_Execution: {
                logList.addAll(this.sqlLogQueue);
                break;
            }
            default: {
                logList.addAll(this.conLogQueue);
                logList.addAll(this.sqlLogQueue);
                break;
            }
        }
        return logList;
    }

    public List<BeeJdbcEventLog> clear(int type) {
        List<BeeJdbcEventLog> removedLogList = new LinkedList<>();
        switch (type) {
            case Type_Connection_Get: {
                conLogQueue.drainTo(removedLogList);
                break;
            }
            case Type_SQL_Execution: {
                sqlLogQueue.drainTo(removedLogList);
                break;
            }
            default: {
                conLogQueue.drainTo(removedLogList);
                sqlLogQueue.drainTo(removedLogList);
                break;
            }
        }

        for (BeeJdbcEventLog log : removedLogList)
            ((DefaultJdbcEventLog) log).setRemoved(true);
        return removedLogList;
    }

    /**
     * Clear timeout logs from manager.
     *
     * @param timeout to check timeout logs
     */
    public void clearTimeout(long timeout) {
        List<BeeJdbcEventLog> handleLogList = null;
        List<BeeJdbcEventLog> conPendingRemovalLogList = new LinkedList<>();
        List<BeeJdbcEventLog> sqlPendingRemovalLogList = new LinkedList<>();
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        if (handleByAsyncMode) {//async mode
            handleLogList = new ArrayList<>(10);
            for (DefaultJdbcEventLog log : conLogQueue) {
                if (currentTime - log.getStartTime() >= timeout) {
                    conPendingRemovalLogList.add(log);
                }

                if (!log.isHandled()) {
                    if (log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold) {
                        log.setAsSlow();
                        handleLogList.add(log);
                    } else if (log.isException()) {
                        handleLogList.add(log);
                    }
                }
            }

            //timeout check on sql execution logs
            for (DefaultJdbcEventLog log : sqlLogQueue) {
                if (currentTime - log.getStartTime() >= timeout) {
                    sqlPendingRemovalLogList.add(log);
                }

                if (!log.isHandled()) {
                    if (log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold) {
                        log.setAsSlow();
                        handleLogList.add(log);
                    } else if (log.isException()) {
                        handleLogList.add(log);
                    }
                }
            }
        } else {
            //timeout check on connection logs
            for (DefaultJdbcEventLog log : conLogQueue) {
                if (currentTime - log.getStartTime() >= timeout) {
                    conPendingRemovalLogList.add(log);
                }
            }

            //timeout check on sql execution logs
            for (DefaultJdbcEventLog log : sqlLogQueue) {
                if (currentTime - log.getStartTime() >= timeout) {
                    sqlPendingRemovalLogList.add(log);
                }
            }
        }

        //2: remove timeout logs from connection log list
        if (!conPendingRemovalLogList.isEmpty() && conLogQueue.removeAll(conPendingRemovalLogList)) {
            for (BeeJdbcEventLog log : conPendingRemovalLogList) {
                ((DefaultJdbcEventLog) log).setRemoved(true);
            }
        }
        //3: remove timeout logs from sql execution log list
        if (!sqlPendingRemovalLogList.isEmpty() && sqlLogQueue.removeAll(sqlPendingRemovalLogList)) {
            for (BeeJdbcEventLog log : sqlPendingRemovalLogList) {
                ((DefaultJdbcEventLog) log).setRemoved(true);
            }
        }

        //4: handle exception logs and slow logs
        if (handleLogList != null && !handleLogList.isEmpty()) {
            try {
                boolean[] flags = this.handler.handle(handleLogList);
                for (int i = 0, l = flags.length; i < l; i++) {
                    ((DefaultJdbcEventLog) (handleLogList.get(i))).setHandled(flags[i]);
                }
            } catch (Throwable e) {
                //do nothing
            }
        }
    }


    /**
     * Start to call a method and a log object is return this start method
     *
     * @param type       is method call type
     * @param method     is method name,for example:getConnection()
     * @param parameters is an array of method parameters
     */
    public BeeJdbcEventLog startCall(int type, String method, Object[] parameters, String sql, Statement statement) {
        DefaultJdbcEventLog log = new DefaultJdbcEventLog(type, method, parameters);
        log.setStartTime(System.currentTimeMillis());
        log.setStatement(statement);
        offerQueue(log, type, parameters, sql);
        return log;
    }

    private void offerQueue(DefaultJdbcEventLog log, int type, Object[] parameters, String sql) {
        if (type == Type_Connection_Get) {
            while (!conLogQueue.offer(log)) {
                if (conLogQueue.size() == this.maxSize) {
                    DefaultJdbcEventLog other = conLogQueue.poll();
                    if (other != null) other.setRemoved(true);
                }
            }
        } else {//Type_Execute_SQL
            if (parameters == null || parameters.length == 0) {
                log.setSql(sql);
            } else {
                log.setSql((String) parameters[0]);
            }

            while (!sqlLogQueue.offer(log)) {
                if (sqlLogQueue.size() == this.maxSize) {
                    DefaultJdbcEventLog other = sqlLogQueue.poll();
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
    public void endCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeJdbcEventLog log) {
        DefaultJdbcEventLog defaultTypeLog = (DefaultJdbcEventLog) log;
        defaultTypeLog.setResult(callResult, preparationTookTime, preparedParameters);
        defaultTypeLog.setEndTime(System.currentTimeMillis());

        if (log.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, log.getType(), log.getParameters(), log.getSql());
        }

        if (((Type_Connection_Get == log.getType() && log.getEndTime() - log.getStartTime() >= slowConnectionGetThreshold)
                || (Type_SQL_Execution == log.getType() && log.getEndTime() - log.getStartTime() >= slowSQLExecutionThreshold))) {
            defaultTypeLog.setAsSlow();
            if (this.handleBySyncMode) {
                try {
                    defaultTypeLog.setHandled(handler.handle(log));
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
    public void endOnException(Throwable failCause, long preparationTookTime, Object[] preparedParameters, BeeJdbcEventLog log) {
        DefaultJdbcEventLog defaultTypeLog = (DefaultJdbcEventLog) log;
        defaultTypeLog.setException(failCause, preparationTookTime, preparedParameters);
        defaultTypeLog.setEndTime(System.currentTimeMillis());

        if (log.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, log.getType(), log.getParameters(), log.getSql());
        }

        if (this.handleBySyncMode) {
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
    public void cancelStatement(Object uuid) throws SQLException {
        for (BeeJdbcEventLog log : sqlLogQueue) {
            if (log.getId().equals(uuid)) {
                log.cancelStatement();
            }
        }
    }
}
