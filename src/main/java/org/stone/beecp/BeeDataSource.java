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

import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beecp.pool.exception.PoolNotCreatedException;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beecp.pool.ConnectionPoolStatics.Dummy_CommonDataSource;
import static org.stone.tools.BeanUtil.createClassInstance;

/**
 * Bee DataSource wrap implementation of {@link BeeConnectionPool}.
 *
 * @author Chris Liao
 * @version 1.0
 */
//fix BeeCP-Starter-#6 Chris-2020-09-01 start
//public final class BeeDataSource extends BeeDataSourceConfig implements DataSource {
public class BeeDataSource extends BeeDataSourceConfig implements DataSource, XADataSource {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private long maxWaitNanos = SECONDS.toNanos(8L);//default vale same to config
    private BeeConnectionPool pool;
    private CommonDataSource subDs;//used to set loginTimeout
    private boolean ready;//true,means that inner pool has created
    private SQLException cause;//inner pool create failed cause

    //***************************************************************************************************************//
    //                                             1:constructors(3)                                                 //
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
            throw new RuntimeException(e);
        }
    }

    private static void createPool(BeeDataSource ds) throws SQLException {
        try {
            BeeConnectionPool pool = (BeeConnectionPool) createClassInstance(ds.getPoolImplementClassName(), BeeConnectionPool.class, "pool");
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
            throw new PoolCreateFailedException("Failed to create a pool with class:" + ds.getPoolImplementClassName(), e);
        }
    }

    //***************************************************************************************************************//
    //                                          2: Connection Getting(pool lazy creation if null)                    //
    //***************************************************************************************************************//
    public Connection getConnection() throws SQLException {
        if (this.ready) return pool.getConnection();
        return createPoolByLock().getConnection();
    }

    public XAConnection getXAConnection() throws SQLException {
        if (this.ready) return pool.getXAConnection();
        return createPoolByLock().getXAConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        if (this.ready) return pool.getConnection(username, password);
        return createPoolByLock().getConnection(username, password);
    }

    public XAConnection getXAConnection(String username, String password) throws SQLException {
        if (this.ready) return pool.getXAConnection(username, password);
        return createPoolByLock().getXAConnection(username, password);
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
    //                                      Override methods from CommonDataSource                                   //
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
    //******************************************************** Override End ******************************************//

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
    //                                     3: below are self-define methods(7)                                       //
    //***************************************************************************************************************//
    public boolean isClosed() {
        return this.pool == null || this.pool.isClosed();
    }

    public void close() {
        if (this.pool != null) this.pool.close();
    }

    //override method
    public void setMaxWait(long maxWait) {
        super.setMaxWait(maxWait);
        this.maxWaitNanos = MILLISECONDS.toNanos(maxWait);
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        if (pool == null) {
            super.setPrintRuntimeLog(printRuntimeLog);//as configuration item
        } else {
            pool.setPrintRuntimeLog(printRuntimeLog);//set to pool
        }
    }

    public BeeConnectionPoolMonitorVo getPoolMonitorVo() throws SQLException {
        return this.getPool().getPoolMonitorVo();
    }

    public Thread[] interruptConnectionCreating(boolean interruptTimeout) throws SQLException {
        return this.getPool().interruptConnectionCreating(interruptTimeout);
    }

    public void clear(boolean forceRecycleBorrowed) throws SQLException {
        this.getPool().clear(forceRecycleBorrowed);
    }

    public void clear(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException {
        if (config == null) throw new BeeDataSourceConfigException("Pool configuration object can't be null");
        this.getPool().clear(forceRecycleBorrowed, config);
        config.copyTo(this);
        this.maxWaitNanos = MILLISECONDS.toNanos(config.getMaxWait());
    }

    private BeeConnectionPool getPool() throws SQLException {
        if (this.pool == null) throw new PoolNotCreatedException("Pool not be created");
        return this.pool;
    }
}
