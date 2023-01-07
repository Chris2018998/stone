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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.stone.beeop.pool.ObjectPoolStatics.PoolClassLoader;

/**
 * object Handle support proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ObjectProxyHandle extends ObjectBaseHandle {
    private Object objectProxy;
    private Exception failCause;

    ObjectProxyHandle(PooledObject p) {
        super(p);
        try {
            this.objectProxy = Proxy.newProxyInstance(
                    PoolClassLoader,
                    p.objectInterfaces,
                    new ObjectReflectHandler(p, this));
        } catch (Exception e) {
            this.failCause = e;
        } catch (Throwable e) {
            this.failCause = new Exception(e);
        }
    }

    public final Object getObjectProxy() throws Exception {
        if (isClosed) throw new ObjectException("No operations allowed after object handle closed");
        if (failCause != null) throw failCause;
        return objectProxy;
    }

    private static final class ObjectReflectHandler implements InvocationHandler {
        private final Object raw;
        private final PooledObject p;
        private final BeeObjectHandle handle;
        private final RawObjectMethodFilter filter;

        ObjectReflectHandler(PooledObject p, ObjectBaseHandle handle) {
            this.p = p;
            this.raw = p.raw;
            this.handle = handle;
            this.filter = p.filter;
        }

        //reflect method
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (handle.isClosed()) throw new ObjectException("No operations allowed after object handle closed");
            if (filter != null) filter.doFilter(method.getName(), method.getParameterTypes(), args);

            Object v = method.invoke(raw, args);
            p.updateAccessTime();
            return v;
        }
    }
}
