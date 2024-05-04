/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeObjectException;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.RawObjectMethodFilter;
import org.stone.beeop.pool.exception.ObjectCallException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.stone.beeop.pool.ObjectPoolStatics.PoolClassLoader;

/**
 * Object proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ObjectProxyHandle extends ObjectSimpleHandle {
    private Object objectProxy;
    private Exception failCause;

    ObjectProxyHandle(PooledObject p) {
        super(p);
    }

    public Object getObjectProxy() throws Exception {
        if (isClosed) throw new BeeObjectException("No operations allowed after object handle closed");
        if (objectProxy != null) return objectProxy;
        if (failCause != null) throw failCause;

        synchronized (this) {
            if (objectProxy != null) return objectProxy;
            if (failCause != null) throw failCause;

            try {
                return this.objectProxy = Proxy.newProxyInstance(
                        PoolClassLoader,
                        p.objectInterfaces,
                        new ObjectReflectHandler(p, this));
            } catch (Exception e) {
                throw failCause = e;
            } catch (Throwable e) {
                throw failCause = new Exception(e);
            }
        }
    }

    private static final class ObjectReflectHandler implements InvocationHandler {
        private final Object raw;
        private final PooledObject p;
        private final BeeObjectHandle handle;
        private final RawObjectMethodFilter filter;

        ObjectReflectHandler(PooledObject p, ObjectSimpleHandle handle) {
            this.p = p;
            this.raw = p.raw;
            this.handle = handle;
            this.filter = p.filter;
        }

        //reflect method
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (handle.isClosed())
                throw new ObjectCallException("No operations allowed after object handle closed");
            if (filter != null) filter.doFilter(p.key, method.getName(), method.getParameterTypes(), args);

            Object v = method.invoke(raw, args);
            p.updateAccessTime();
            return v;
        }
    }
}
