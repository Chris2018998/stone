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

import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.FastConnectionPool4L;
import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;
import org.stone.tools.BeanUtil;
import org.stone.tools.extension.InterruptionReentrantReadWriteLock;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.Closeable;
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
import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;
import static org.stone.tools.LogPrinter.CommonLogPrinter;

/**
 * Bee DataSource wrap implementation of {@link BeeConnectionPool}.
 *
 * @author Chris Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource, Closeable {
    private final InterruptionReentrantReadWriteLock lock = new InterruptionReentrantReadWriteLock();
    private final InterruptionReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = 8000L;//default vale same to config
    private BeeConnectionPool pool;
    private CommonDataSource subDs;//used to set loginTimeout
    private boolean poolCreated;//true,means that inner pool has created
    private SQLException cause;//inner pool create failed cause

    //***************************************************************************************************************//
    //                                         1:constructors(3)                                                     //
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
            throw new BeeDataSourceCreationException(e);
        }
    }

    private static void createPool(BeeDataSource ds) throws SQLException {
        String poolImplementClassName = ds.getPoolImplementClassName();
        try {
            if (isBlank(poolImplementClassName)) {
                poolImplementClassName = (ds.getEventLogManager() != null || ds.getEventlogManagerClass() != null || isNotBlank(ds.getEventLogManagerClassName())) ?
                        FastConnectionPool4L.class.getName() : FastConnectionPool.class.getName();
            }

            ds.pool = (BeeConnectionPool) createClassInstance(poolImplementClassName, BeeConnectionPool.class, "pool");
            ds.pool.start(ds);

            Object connectionFactory = ds.getConnectionFactory();
            if (connectionFactory instanceof CommonDataSource)
                ds.subDs = (CommonDataSource) connectionFactory;
            else
                ds.subDs = Dummy_CommonDataSource;

            ds.poolCreated = true;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            throw new PoolCreateFailedException("Failed to create a pool with class:" + poolImplementClassName, e);
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
    //                                         2: Pooled connections get                                             //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (this.poolCreated) return pool.getConnection();
        return createPoolByLock().getConnection();
    }

    public XAConnection getXAConnection() throws SQLException {
        if (this.poolCreated) return pool.getXAConnection();
        return createPoolByLock().getXAConnection();
    }

    public Connection getConnection(String user, String password) throws SQLException {
        CommonLogPrinter.info("getConnection (user,password) ignores authentication - returning default connection");
        return getConnection();
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        CommonLogPrinter.info("getXAConnection (user,password) ignores authentication - returning default XAConnection");
        return getXAConnection();
    }

    private BeeConnectionPool createPoolByLock() throws SQLException {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!poolCreated) {
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
    //                                         3: Pool clear(2)                                                      //
    //***************************************************************************************************************//
    public void restart(boolean forceRecycleBorrowed) throws SQLException {
        this.getPool().restart(forceRecycleBorrowed);
    }

    public void restart(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        this.getPool().restart(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    //***************************************************************************************************************//
    //                                         4: Override methods of CommonDataSource                              //
    //***************************************************************************************************************//
    public PrintWriter getLogWriter() throws SQLException {
        return poolCreated ? subDs.getLogWriter() : null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        if (poolCreated) subDs.setLogWriter(out);
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return poolCreated ? subDs.getParentLogger() : null;
    }

    public int getLoginTimeout() throws SQLException {
        return poolCreated ? subDs.getLoginTimeout() : 0;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        if (poolCreated) subDs.setLoginTimeout(seconds);
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
    //                                         5: runtime logs print(4)                                              //
    //***************************************************************************************************************//
    public boolean isPrintRuntimeLogs() {
        if (poolCreated) {
            return pool.isEnabledLogPrint();
        } else {
            return super.isPrintRuntimeLogs();
        }
    }

    public void setPrintRuntimeLogs(boolean enable) {
        if (poolCreated) {
            pool.enableLogPrint(enable);//set to pool
        } else {
            super.setPrintRuntimeLogs(enable);//as configuration item
        }
    }

    public boolean isEnabledLogPrint() throws SQLException {
        return this.getPool().isEnabledLogPrint();
    }

    public void enableLogPrint(boolean enable) throws SQLException {
        this.getPool().enableLogPrint(enable);
    }

    //***************************************************************************************************************//
    //                                         6: jdbc event logs manager(5)                                          //
    //***************************************************************************************************************//
    public boolean isEnabledEventLogManager() throws SQLException {
        return this.getPool().isEnabledEventLogManager();
    }

    public void enableEventLogManager(boolean enable) throws SQLException {
        this.getPool().enableEventLogManager(enable);
    }

    public List<BeeJdbcEventLog> getEventLog(int type) throws SQLException {
        return this.getPool().getEventLog(type);
    }

    public List<BeeJdbcEventLog> clearEventLog(int type) throws SQLException {
        return this.getPool().clearEventLog(type);
    }

    public boolean cancelStatement(Object logId) throws SQLException {
        return this.getPool().cancelStatement(logId);
    }

    //***************************************************************************************************************//
    //                                     7: override methods to set or update jdbc link info                       //
    //***************************************************************************************************************//
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

    //***************************************************************************************************************//
    //                                         8: other methods(7)                                                   //
    //***************************************************************************************************************//
    public void close() {
        if (this.poolCreated) this.pool.close();
    }

    public boolean isClosed() {
        return !this.poolCreated || this.pool.isClosed();
    }

    public boolean isReady() {
        return this.poolCreated && this.pool.isReady();
    }

    //override method
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        return this.getPool().getPoolMonitorVo();
    }

    public List<Thread> interruptWaitingThreads() throws SQLException {
        if (this.poolCreated) {
            return pool.interruptWaitingThreads();
        } else {
            return lock.interruptAllThreads();
        }
    }

    private BeeConnectionPool getPool() throws SQLException {
        if (!this.poolCreated) throw new PoolNotCreatedException("Internal pool was not ready");
        return this.pool;
    }
}
