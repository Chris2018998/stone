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

import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.RawObjectMethodFilter;
import org.stone.beeop.pool.exception.ObjectException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static org.stone.beeop.pool.ObjectPoolStatics.EMPTY_CLASSES;
import static org.stone.beeop.pool.ObjectPoolStatics.EMPTY_CLASS_NAMES;

/**
 * object Handle implement
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectBaseHandle implements BeeObjectHandle {
    private static final ConcurrentHashMap<MethodCacheKey, Method> MethodMap = new ConcurrentHashMap<MethodCacheKey, Method>(16);
    protected final PooledObject p;
    private final Object raw;
    private final RawObjectMethodFilter filter;
    protected boolean isClosed;

    ObjectBaseHandle(PooledObject p) {
        this.p = p;
        this.raw = p.raw;
        p.handleInUsing = this;
        this.filter = p.filter;
    }

    //***************************************************************************************************************//
    //                                  1: override methods(4)                                                       //                                                                                  //
    //***************************************************************************************************************//
    public String toString() {
        return p.toString();
    }

    public boolean isClosed() {
        return isClosed;
    }

    public final void close() throws Exception {
        synchronized (this) {//safe close
            if (isClosed) return;
            isClosed = true;
        }
        p.recycleSelf();
    }

    //***************************************************************************************************************//
    //                                 2: raw methods call methods(2)                                                //                                                                                  //
    //***************************************************************************************************************//
    public Object getObjectProxy() throws Exception {
        return null;
    }

    public Object call(String methodName) throws Exception {
        return call(methodName, EMPTY_CLASSES, EMPTY_CLASS_NAMES);
    }

    public Object call(String name, Class[] types, Object[] params) throws Exception {
        if (isClosed) throw new ObjectException("No operations allowed after object handle closed");
        if (filter != null) filter.doFilter(name, types, params);

        MethodCacheKey key = new MethodCacheKey(name, types);
        Method method = MethodMap.get(key);

        if (method == null) {
            method = p.rawClass.getMethod(name, types);
            MethodMap.put(key, method);
        }

        Object v = method.invoke(raw, params);
        p.updateAccessTime();
        return v;
    }

    //***************************************************************************************************************//
    //                                  4: inner class(1)                                                            //                                                                                  //
    //***************************************************************************************************************//
    private static final class MethodCacheKey {
        private final String name;
        private final Class[] types;

        MethodCacheKey(String name, Class[] types) {
            this.name = name;
            this.types = types;
        }

        public int hashCode() {
            int result = this.name.hashCode();
            result = 31 * result + Arrays.hashCode(this.types);
            return result;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            MethodCacheKey that = (MethodCacheKey) o;
            return this.name.equals(that.name) &&
                    Arrays.equals(this.types, that.types);
        }
    }
}
