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
import org.stone.beecp.pool.exception.PoolCreateFailedException;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPool;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.util.atomic.IntegerFieldUpdaterImpl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * keyed object pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class KeyedObjectPool implements BeeObjectPool {
    private static final Logger Log = LoggerFactory.getLogger(KeyedObjectPool.class);
    private static final AtomicIntegerFieldUpdater<KeyedObjectPool> PoolStateUpd = IntegerFieldUpdaterImpl.newUpdater(KeyedObjectPool.class, "poolState");
    private final Map<Object, ObjectGenericPool> genericPoolMap = new ConcurrentHashMap<>(1);

    private String poolName;
    private volatile int poolState;
    private ObjectPoolHook exitHook;
    private BeeObjectSourceConfig poolConfig;
    private ObjectGenericPool cloneGenericPool;
    private ObjectGenericPool defaultGenericPool;//key is null
    private ThreadPoolExecutor servantService;
    private IdleClearTask scheduledIdleClearTask;
    private ScheduledThreadPoolExecutor scheduledService;

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//
    public void init(BeeObjectSourceConfig config) throws Exception {
        //step1: pool check
        if (config == null) throw new PoolCreateFailedException("Configuration can't be null");
        if (!PoolStateUpd.compareAndSet(this, POOL_NEW, POOL_STARTING))
            throw new PoolCreateFailedException("Pool has been initialized or in starting");
        this.poolConfig = config.check();

        //step2: create clone pool
        this.poolName = poolConfig.getPoolName();
        this.cloneGenericPool = new ObjectGenericPool(poolConfig, this);

        //step3: register pool exit hook
        this.exitHook = new ObjectPoolHook(this);
        Runtime.getRuntime().addShutdownHook(this.exitHook);

        //step4: calculate pool thread size and prepare thread factory
        int maxObjectKeySize = config.getMaxObjectKeySize();
        int cpuCoreSize = Runtime.getRuntime().availableProcessors();
        int coreThreadSize = Math.min(cpuCoreSize, maxObjectKeySize);
        PoolThreadFactory poolThreadFactory = new PoolThreadFactory(poolName);

        //step5: create servant executor(core thread keep alive)
        this.servantService = new ThreadPoolExecutor(coreThreadSize, coreThreadSize, 15,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(maxObjectKeySize), poolThreadFactory);

        //step6: create idle scheduled executor(core thread keep alive)
        this.scheduledIdleClearTask = new IdleClearTask(this);
        scheduledService = new ScheduledThreadPoolExecutor(coreThreadSize, poolThreadFactory);
        scheduledService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduledService.setRemoveOnCancelPolicy(true);
        scheduledService.scheduleWithFixedDelay(scheduledIdleClearTask, 0, config.getTimerCheckInterval(), TimeUnit.MILLISECONDS);

        //step7: create generic pool by init size
        try {
            if (config.getInitialSize() > 0) {
                ObjectGenericPool genericPool = cloneGenericPool.createByClone(config.getInitialObjectKey(),
                        poolName, config.getInitialSize(), config.isAsyncCreateInitObject());

                genericPoolMap.put(config.getInitialObjectKey(), genericPool);
            }
        } finally {
            this.poolState = POOL_READY;//assume that default pool create failed,the state should be ready
        }
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
        ObjectGenericPool pool = genericPoolMap.get(key);
        return pool.getObjectHandle();
    }

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//
    //check pool is whether closed
    public boolean isClosed() {
        return this.poolState == POOL_CLOSED;
    }

    //close pool
    public void close() {

    }

    //get pool monitor vo
    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    //get pool monitor vo
    public BeeObjectPoolMonitorVo getPoolMonitorVo(Object key) {
        return null;
    }

    //enable Runtime Log
    public void setPrintRuntimeLog(boolean indicator) {

    }

    //enable Runtime Log
    public void setPrintRuntimeLog(Object key, boolean indicator) {

    }

    //remove all objects from pool
    public void clear(boolean forceCloseUsing) throws Exception {

    }

    //remove all objects from pool
    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {

    }

    void submitAsyncServantTask(Runnable task) {

    }

    void closeIdleTimeout() {
        Iterator<ObjectGenericPool> iterator = genericPoolMap.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().closeIdleTimeout();
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


    //    //Method-5.8: assembly pool to jmx
//    private void registerJmx() {
//        if (this.poolConfig.isEnableJmx()) {
//            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            this.registerJmxBean(mBeanServer, String.format("FastObjectPool:type=BeeOP(%s)", this.poolName), this);
//            this.registerJmxBean(mBeanServer, String.format("BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName), this.poolConfig);
//        }
//    }
//
//    //Method-5.9: jmx assembly
//    private void registerJmxBean(MBeanServer mBeanServer, String regName, Object bean) {
//        try {
//            ObjectName jmxRegName = new ObjectName(regName);
//            if (!mBeanServer.isRegistered(jmxRegName)) {
//                mBeanServer.registerMBean(bean, jmxRegName);
//            }
//        } catch (Exception e) {
//            Log.warn("BeeOP({})failed to assembly jmx-bean:{}", this.poolName, regName, e);
//        }
//    }
//
//    //Method-5.10: pool unregister from jmx
//    private void unregisterJmx() {
//        if (this.poolConfig.isEnableJmx()) {
//            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//            this.unregisterJmxBean(mBeanServer, String.format("FastObjectPool:type=BeeOP(%s)", this.poolName));
//            this.unregisterJmxBean(mBeanServer, String.format("BeeObjectSourceConfig:type=BeeOP(%s)-config", this.poolName));
//        }
//    }
//
//    //Method-5.11: jmx unregister
//    private void unregisterJmxBean(MBeanServer mBeanServer, String regName) {
//        try {
//            ObjectName jmxRegName = new ObjectName(regName);
//            if (mBeanServer.isRegistered(jmxRegName)) {
//                mBeanServer.unregisterMBean(jmxRegName);
//            }
//        } catch (Exception e) {
//            Log.warn("BeeOP({})failed to unregister jmx-bean:{}", this.poolName, regName, e);
//        }
//    }

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
    private static class IdleClearTask implements Runnable {
        private final KeyedObjectPool pool;

        IdleClearTask(KeyedObjectPool pool) {
            this.pool = pool;
        }

        public void run() {
            try {
                this.pool.closeIdleTimeout();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    private static final class PoolThreadFactory implements ThreadFactory {
        private final String threadName;

        PoolThreadFactory(String threadName) {
            this.threadName = threadName;
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        }
    }

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
