/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class KeyedObjectPool implements BeeObjectPool, BeeObjectPoolJmxBean {
    private static final Logger Log = LoggerFactory.getLogger(KeyedObjectPool.class);
    private final Map<Object, ObjectGenericPool> subPoolMap = new ConcurrentHashMap<>();
    private String poolName;
    private BeeObjectSourceConfig poolConfig;

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeObjectSourceConfig config) throws Exception {
        this.poolConfig = config;
        this.poolName = poolConfig.getPoolName();
    }

    //***************************************************************************************************************//
    //                2: objects methods(2)                                                                          //                                                                                  //
    //***************************************************************************************************************//
    //borrow a object from pool
    public BeeObjectHandle getObjectHandle() throws Exception {
        return getObjectHandle(null);
    }

    //borrow a object from pool
    public BeeObjectHandle getObjectHandle(Object key) throws Exception {
        ObjectGenericPool pool = subPoolMap.get(key);
        return pool.getObjectHandle();
    }

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //close pool
    public void close() {

    }

    //check pool is closed
    public boolean isClosed() {
        return false;
    }

    //get pool monitor vo
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    //enable Runtime Log
    public void setPrintRuntimeLog(boolean indicator) {

    }

    //remove all objects from pool
    public void restart(boolean forceCloseUsing) throws Exception {

    }

    //remove all objects from pool
    public void restart(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {

    }


    //Method-5.8: assembly pool to jmx
    private void registerJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.registerJmxBean(mBeanServer, String.format("FastObjectPool:type=BeeOP(%s)", this.poolName), this);
            this.registerJmxBean(mBeanServer, String.format("BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName), this.poolConfig);
        }
    }

    //Method-5.9: jmx assembly
    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (!mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.registerMBean(bean, jmxRegName);
            }
        } catch (Exception e) {
            Log.warn("BeeOP({})failed to assembly jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //Method-5.10: pool unregister from jmx
    private void unregisterJmx() {
        if (this.poolConfig.isEnableJmx()) {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            this.unregisterJmxBean(mBeanServer, String.format("FastObjectPool:type=BeeOP(%s)", this.poolName));
            this.unregisterJmxBean(mBeanServer, String.format("BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName));
        }
    }

    //Method-5.11: jmx unregister
    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
        try {
            ObjectName jmxRegName = new ObjectName(regName);
            if (mBeanServer.isRegistered(jmxRegName)) {
                mBeanServer.unregisterMBean(jmxRegName);
            }
        } catch (Exception e) {
            Log.warn("BeeOP({})failed to unregister jmx-bean:{}", this.poolName, regName, e);
        }
    }

    //***************************************************************************************************************//
    //                3: Jmx methods(6)                                                                              //                                                                                  //
    //***************************************************************************************************************//
    //return current size(using +idle)
    public int getTotalSize() {
        //@todo
        return 0;
    }

    //return idle size
    public int getIdleSize() {
        //@todo
        return 0;
    }

    //return using size
    public int getUsingSize() {
        //@todo
        return 0;
    }

    //return semaphore acquired success size from pool
    public int getSemaphoreAcquiredSize() {
        //@todo
        return 0;
    }

    //return waiting size to take semaphore synchronizer
    public int getSemaphoreWaitingSize() {
        //@todo
        return 0;
    }

    //return waiter size for transferred object
    public int getTransferWaitingSize() {
        //@todo
        return 0;
    }

    //***************************************************************************************************************//
    //                                  6: Pool inner interface/class(8)                                             //                                                                                  //
    //***************************************************************************************************************//
    //class-6.8: JVM exit hook
    private static class ObjectPoolHook extends Thread {
        private final KeyedObjectPool pool;

        ObjectPoolHook(KeyedObjectPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                Log.info("BeeOP({})exit-hook running", this.pool.poolName);
                this.pool.close();
            } catch (Throwable e) {
                Log.error("BeeOP({})Error at closing pool,cause:", this.pool.poolName, e);
            }
        }
    }
}
