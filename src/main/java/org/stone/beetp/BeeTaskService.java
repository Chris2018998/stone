/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Task Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BeeTaskService extends BeeTaskServiceConfig {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private BeeTaskPool pool;
    private boolean ready;
    private Exception cause;

    //***************************************************************************************************************//
    //                                             1:constructors(2)                                                 //
    //***************************************************************************************************************//
    public BeeTaskService() {
    }

    public BeeTaskService(BeeTaskServiceConfig config) {
        try {
            config.copyTo(this);
            createPool(this);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createPool(BeeTaskService service) throws Exception {
        Class<?> poolClass = Class.forName(service.getPoolImplementClassName());
        if (BeeTaskPool.class.isAssignableFrom(poolClass))
            throw new BeeTaskServiceConfigException("Invalid pool implement class name:" + service.getPoolImplementClassName());

        BeeTaskPool pool = (BeeTaskPool) poolClass.newInstance();
        pool.init(service);
        service.pool = pool;
        service.ready = true;
    }

    //***************************************************************************************************************//
    //                                        2: task submit methods(3)                                              //
    //***************************************************************************************************************//
    public BeeTaskHandle submit(BeeTask task) throws Exception {
        if (this.ready) return pool.submit(task);
        return createPoolByLock().submit(task);
    }

    public List<BeeTaskHandle> submit(List<BeeTask> taskList) throws Exception {
        if (this.ready) return pool.submit(taskList);
        return createPoolByLock().submit(taskList);
    }

    private BeeTaskPool createPoolByLock() throws Exception {
        if (!lock.isWriteLocked() && lock.writeLock().tryLock()) {
            try {
                if (!ready) {
                    cause = null;
                    createPool(this);
                }
            } catch (Exception e) {
                cause = e;
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            readLock.lock();
            readLock.unlock();
        }

        //read lock will reach
        if (cause != null) throw cause;
        return pool;
    }

    //***************************************************************************************************************//
    //                                        3: task submit methods(3)                                              //
    //***************************************************************************************************************//


}