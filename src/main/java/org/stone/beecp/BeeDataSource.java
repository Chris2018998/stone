/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import org.stone.beecp.exception.*;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.FastConnectionPoolMonitorVo;
import org.stone.tools.BeanUtil;
import org.stone.tools.extension.InterruptableReentrantReadWriteLock;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.Dummy_CommonDataSource;
import static org.stone.beecp.pool.ConnectionPoolStatics.POOL_UNCREATED;
import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.LogPrinter.DefaultLogPrinter;

/**
 * Bee DataSource wrap implementation of {@link BeeConnectionPool}.
 *
 * @author Chris Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource, AutoCloseable {
    private final InterruptableReentrantReadWriteLock lock = new InterruptableReentrantReadWriteLock();
    private final InterruptableReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = 8000L;//default vale same to config
    private BeeConnectionPool pool;

    private CommonDataSource subDs;//used to set loginTimeout
    private boolean poolStarted;//true,means that inner pool has created
    private SQLException cause;//inner pool create failed cause

    //***************************************************************************************************************//
    //                                         0:constructors(3+2)                                                   //
    //***************************************************************************************************************//
    public BeeDataSource() {
    }

    public BeeDataSource(String driver, String url, String user, String password) {
        super(driver, url, user, password);
    }

    public BeeDataSource(BeeDataSourceConfig config) {
        try {
            config.copyTo(this);
            BeeDataSource.createPool(this);
            this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
        } catch (SQLException e) {
            throw new BeeDataSourceCreatedException(e);
        }
    }

    private static void createPool(BeeDataSource ds) throws SQLException {
        String poolImplementClassName = ds.getPoolImplementClassName();
        if (isBlank(poolImplementClassName)) poolImplementClassName = FastConnectionPool.class.getName();
        try {
            ds.pool = createClassInstance(poolImplementClassName, BeeConnectionPool.class, "pool");
            ds.pool.start(ds);
            ds.poolStarted = true;

            Object connectionFactory = ds.getConnectionFactory();
            if (connectionFactory instanceof CommonDataSource)
                ds.subDs = (CommonDataSource) connectionFactory;
            else
                ds.subDs = Dummy_CommonDataSource;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeDataSourcePoolInstantiatedException("Failed to create a pool with class:" + poolImplementClassName, e);
        }
    }

    private static void set(Object target, String setMethodName, String value) {
        try {
            Method method = target.getClass().getMethod(setMethodName, String.class);
            BeanUtil.setAccessible(target, method);
            method.invoke(target, value);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    //***************************************************************************************************************//
    //                                         1: Pooled connections get(4+1)                                        //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (this.poolStarted) return pool.getConnection();
        return createPoolByLock().getConnection();
    }

    public XAConnection getXAConnection() throws SQLException {
        if (this.poolStarted) return pool.getXAConnection();
        return createPoolByLock().getXAConnection();
    }

    public Connection getConnection(String user, String password) throws SQLException {
        DefaultLogPrinter.info("getConnection (user,password) ignores authentication - returning default connection");
        return getConnection();
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        DefaultLogPrinter.info("getXAConnection (user,password) ignores authentication - returning default XAConnection");
        return getXAConnection();
    }

    private BeeConnectionPool createPoolByLock() throws SQLException {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!poolStarted) {
                    cause = null;
                    createPool(this);
                }
            } catch (SQLException e) {
                cause = e;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            try {
                if (!this.readLock.tryLock(maxWaitNanos, TimeUnit.NANOSECONDS))
                    throw new ConnectionGetTimeoutException("Timeout on waiting for pool ready");
            } catch (InterruptedException e) {
                throw new ConnectionGetInterruptedException("An interruption occurred while waiting for pool ready");
            }
            readLock.unlock();
        }

        if (cause != null) throw cause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                         2: Method execution logs cache(5)                                     //
    //***************************************************************************************************************//
    public void enableMethodExecutionLogCache(boolean enable) throws SQLException {
        this.getPool().enableMethodExecutionLogCache(enable);
    }

    public List<BeeMethodExecutionLog> getMethodExecutionLogs(int type) throws SQLException {
        return this.getPool().getMethodExecutionLogs(type);
    }

    public List<BeeMethodExecutionLog> clearMethodExecutionLogs(int type) throws SQLException {
        return this.getPool().clearMethodExecutionLogs(type);
    }

    public boolean cancelStatement(String logId) throws SQLException {
        return this.getPool().cancelStatement(logId);
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener listener) {
        if (poolStarted) {
            pool.setMethodExecutionListener(listener);//set to pool
        } else {
            super.setMethodExecutionListener(listener);//as configuration item
        }
    }

    //***************************************************************************************************************//
    //                                         3: Pool maintenance(6)                                                //
    //***************************************************************************************************************//
    public void close() {
        if (this.poolStarted) this.pool.close();
    }

    public boolean isClosed() {
        return !this.poolStarted || this.pool.isClosed();
    }

    public boolean suspend() {
        return this.poolStarted && this.pool.suspendPool();
    }

    public boolean resume() {
        return this.poolStarted && this.pool.resumePool();
    }

    public void restart(boolean forceRecycleBorrowed) throws SQLException {
        this.getPool().restart(forceRecycleBorrowed);
    }

    public void restart(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        this.getPool().restart(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    //***************************************************************************************************************//
    //                                         4: Pool others(4)                                                     //
    //***************************************************************************************************************//
    public void enableLogPrint(boolean enable) throws SQLException {
        this.getPool().enableLogPrint(enable);
    }

    public List<Thread> interruptWaitingThreads() {
        if (pool != null) {
            return pool.interruptWaitingThreads();
        } else {
            return lock.interruptAllThreads();
        }
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        if (pool != null) {
            return pool.getPoolMonitorVo();
        } else {
            return new FastConnectionPoolMonitorVo(
                    this.getPoolName(),
                    this.isFairMode(),
                    this.getMaxActive(),
                    this.getSemaphoreSize(),
                    this.isUseThreadLocal(),
                    POOL_UNCREATED,
                    0,
                    0,
                    this.getSemaphoreSize(),
                    0,
                    0,
                    0,
                    0,
                    this.isPrintRuntimeLogs(),
                    this.isEnableMethodExecutionLogCache());
        }
    }

    //***************************************************************************************************************//
    //                                         5: Override methods of configuration                                  //
    //***************************************************************************************************************//
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }

    public void setUsername(String username) {
        if (subDs == null) {
            super.setUsername(username);
        } else {
            set(subDs, "setUsername", username);
        }
    }

    public void setPassword(String password) {
        if (subDs == null) {
            super.setPassword(password);
        } else {
            set(subDs, "setPassword", password);
        }
    }

    public void setJdbcUrl(String jdbcUrl) {
        if (subDs == null) {
            super.setJdbcUrl(jdbcUrl);
        } else {
            set(subDs, "setJdbcUrl", jdbcUrl);
        }
    }

    public void setUrl(String jdbcUrl) {
        if (subDs == null) {
            super.setUrl(jdbcUrl);
        } else {
            set(subDs, "setUrl", jdbcUrl);
        }
    }

    public boolean isPrintRuntimeLogs() {
        if (poolStarted) {
            return pool.getPoolMonitorVo().isEnabledLogPrint();
        } else {
            return super.isPrintRuntimeLogs();
        }
    }

    public void setPrintRuntimeLogs(boolean enable) {
        if (poolStarted) {
            pool.enableLogPrint(enable);//set to pool
        } else {
            super.setPrintRuntimeLogs(enable);//as configuration item
        }
    }

    //***************************************************************************************************************//
    //                                         6: Override methods of CommonDataSource                               //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        return subDs != null ? subDs.getLogWriter() : null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        if (subDs != null) subDs.setLogWriter(out);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return subDs != null ? subDs.getParentLogger() : null;
    }

    public int getLoginTimeout() throws SQLException {
        return subDs != null ? subDs.getLoginTimeout() : 0;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        if (subDs != null) subDs.setLoginTimeout(seconds);
    }

    public boolean isWrapperFor(Class<?> clazz) {
        return clazz != null && clazz.isInstance(this);
    }

    public <T> T unwrap(Class<T> clazz) throws SQLException {
        if (clazz != null && clazz.isInstance(this))
            return clazz.cast(this);
        else
            throw new SQLException("The wrapper object was not an instance of " + clazz);
    }

    //***************************************************************************************************************//
    //                                         7: Private methods                                                    //
    //***************************************************************************************************************//
    private BeeConnectionPool getPool() throws SQLException {
        if (!this.poolStarted) throw new BeeDataSourcePoolNotInstantiatedException("Datasource pool not instantiated");
        return this.pool;
    }
}


