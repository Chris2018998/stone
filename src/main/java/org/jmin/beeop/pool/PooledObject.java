/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.pool;

import org.jmin.beeop.RawObjectFactory;

import java.util.Set;

import static java.lang.System.currentTimeMillis;

/**
 * Pooled Entry
 *
 * @author Chris Liao
 * @version 1.0
 */
final class PooledObject implements Cloneable {
    final Class[] objectInterfaces;
    final Set<String> excludeMethodNames;
    private final ObjectPool pool;
    private final RawObjectFactory factory;

    Object raw;
    Class rawClass;
    volatile int state;
    ObjectBaseHandle handleInUsing;
    volatile long lastAccessTime;

    //***************************************************************************************************************//
    //                                  1: Pooled entry create/clone methods(2)                                      //                                                                                  //
    //***************************************************************************************************************//
    PooledObject(ObjectPool pool, RawObjectFactory factory, Class[] objectInterfaces, Set<String> excludeMethodNames) {
        this.pool = pool;
        this.factory = factory;
        this.objectInterfaces = objectInterfaces;
        this.excludeMethodNames = excludeMethodNames;
    }

    PooledObject setDefaultAndCopy(Object raw, int state) throws Exception {
        this.factory.setDefault(raw);
        PooledObject p = (PooledObject) this.clone();

        p.raw = raw;
        p.rawClass = raw.getClass();
        p.state = state;
        p.lastAccessTime = currentTimeMillis();//first time
        return p;
    }

    //***************************************************************************************************************//
    //                               2: Pooled entry business methods(4)                                             //                                                                                  //
    //***************************************************************************************************************//
    public String toString() {
        return this.raw.toString();
    }

    void updateAccessTime() {
        this.lastAccessTime = currentTimeMillis();
    }

    //called by pool before remove from pool
    void onBeforeRemove() {
        try {
            state = ObjectPoolStatics.OBJECT_CLOSED;
        } catch (Throwable e) {
            ObjectPoolStatics.CommonLog.error("Object close error", e);
        } finally {
            this.factory.destroy(this.raw);
        }
    }

    void recycleSelf() throws Exception {
        try {
            this.handleInUsing = null;
            this.factory.reset(this.raw);
            this.pool.recycle(this);
        } catch (Throwable e) {
            this.pool.abandonOnReturn(this);
            if (e instanceof Exception)
                throw (Exception) e;
            else
                throw new Exception(e);
        }
    }
}