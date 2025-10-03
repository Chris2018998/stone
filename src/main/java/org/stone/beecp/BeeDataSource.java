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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.Dummy_CommonDataSource;
import static org.stone.tools.BeanUtil.CommonLog;
import static org.stone.tools.BeanUtil.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Bee DataSource wrap implementation of {@link BeeConnectionPool}.
 *
 * @author Chris Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource, Closeable {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = SECONDS.toNanos(8L);//default vale same to config
    private BeeConnectionPool pool;
    private CommonDataSource subDs;//used to set loginTimeout
    private boolean ready;//true,means that inner pool has created
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
                poolImplementClassName = (ds.getJdbcCallLogCollector() != null || ds.getJdbcCallLogCollectorClass() != null || isNotBlank(ds.getJdbcCallLogCollectorClassName())) ?
                        FastConnectionPool4L.class.getName() : FastConnectionPool.class.getName();
            }

            BeeConnectionPool pool = (BeeConnectionPool) createClassInstance(poolImplementClassName, BeeConnectionPool.class, "pool");
            pool.init(ds);
            ds.pool = pool;

            Object connectionFactory = ds.getConnectionFactory();
            if (connectionFactory instanceof CommonDataSource)
                ds.subDs = (CommonDataSource) connectionFactory;
            else
                ds.subDs = Dummy_CommonDataSource;

            ds.ready = true;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            throw new PoolCreateFailedException("Failed to create a pool with class:" + poolImplementClassName, e);
        }
    }

    //***************************************************************************************************************//
    //                                         2: Pooled connections get                                             //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (this.ready) return pool.getConnection();
        return createPoolByLock().getConnection();
    }

    public XAConnection getXAConnection() throws SQLException {
        if (this.ready) return pool.getXAConnection();
        return createPoolByLock().getXAConnection();
    }

    public Connection getConnection(String user, String password) throws SQLException {
        CommonLog.info("getConnection (user,password) ignores authentication - returning default connection");
        return getConnection();
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        CommonLog.info("getXAConnection (user,password) ignores authentication - returning default XAConnection");
        return getXAConnection();
    }

    private BeeConnectionPool createPoolByLock() throws SQLException {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
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
    public void clear(boolean forceRecycleBorrowed) throws SQLException {
        this.getPool().clear(forceRecycleBorrowed);
    }

    public void clear(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        if (config == null) throw new BeeDataSourceConfigException("Pool configuration object can't be null");
        this.getPool().clear(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    //***************************************************************************************************************//
    //                                         4: Override methods of CommonDataSource                              //
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
    //                                         5: log print                                                          //
    //***************************************************************************************************************//
    public boolean isPrintRuntimeLog() {
        if (pool == null) {
            return super.isPrintRuntimeLog();
        } else {
            return pool.isEnabledLogPrint();
        }
    }

    public void setPrintRuntimeLog(boolean enable) {
        if (pool == null) {
            super.setPrintRuntimeLog(enable);//as configuration item
        } else {
            pool.enableLogPrint(enable);//set to pool
        }
    }

    public boolean isEnabledLogPrint() throws SQLException {
        return this.getPool().isEnabledLogPrint();
    }

    public void enableLogPrint(boolean enable) throws SQLException {
        this.getPool().enableLogPrint(enable);
    }

    //***************************************************************************************************************//
    //                                         6: JDBC method log collector                                          //
    //***************************************************************************************************************//
    public boolean isEnabledJdbcCallLogCollector() throws SQLException {
        return this.getPool().isEnabledJdbcCallLogCollector();
    }

    public void enableJdbcCallLogCollector(boolean enable) throws SQLException {
        this.getPool().enableJdbcCallLogCollector(enable);
    }

    public List<BeeJdbcCallLog> getJdbcCallLog(int type) throws SQLException {
        return this.getPool().getJdbcCallLog(type);
    }

    public void clearJdbcCallLog() throws SQLException {
        this.getPool().clearJdbcCallLog();
    }

    //***************************************************************************************************************//
    //                                     7: override set methods of jdbc info                                      //
    //***************************************************************************************************************//
    public void setUsername(String username) {
        if (pool == null) {
            super.setUsername(username);
        } else {
            set(subDs, "setUsername", username);
        }
    }

    public void setPassword(String password) {
        if (pool == null) {
            super.setPassword(password);
        } else {
            set(subDs, "setPassword", password);
        }
    }

    public void setJdbcUrl(String jdbcUrl) {
        if (pool == null) {
            super.setJdbcUrl(jdbcUrl);
        } else {
            set(subDs, "setJdbcUrl", jdbcUrl);
        }
    }

    public void setUrl(String jdbcUrl) {
        setJdbcUrl(jdbcUrl);
    }

    private void set(Object target, String setMethodName, String value) {
        try {
            Method method = target.getClass().getMethod(setMethodName, String.class);
            BeanUtil.setAccessible(target, method);
            method.invoke(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //***************************************************************************************************************//
    //                                         8: other methods(7)                                                   //
    //***************************************************************************************************************//
    public void close() {
        if (this.pool != null) this.pool.close();
    }

    public boolean isClosed() {
        return this.pool == null || this.pool.isClosed();
    }

    //override method
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        return this.getPool().getPoolMonitorVo();
    }

    public Thread[] interruptConnectionCreating(boolean interruptTimeout) throws SQLException {
        return this.getPool().interruptConnectionCreating(interruptTimeout);
    }

    private BeeConnectionPool getPool() throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Data source pool not be instantiated");
        return this.pool;
    }
}
