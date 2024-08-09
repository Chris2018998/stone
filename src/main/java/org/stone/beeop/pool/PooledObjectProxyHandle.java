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

import org.stone.beeop.BeeObjectMethodFilter;
import org.stone.beeop.BeeObjectPredicate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.stone.beeop.pool.ObjectPoolStatics.DESC_RM_BAD;
import static org.stone.tools.CommonUtil.isNotBlank;

/**
 * Object proxy
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PooledObjectProxyHandle extends PooledObjectPlainHandle {
    private final Object objectProxy;

    PooledObjectProxyHandle(PooledObject p, BeeObjectPredicate predicate, ClassLoader poolClassLoader,
                            Class<?>[] objectInterfaces, BeeObjectMethodFilter methodFilter) {
        super(p, predicate);
        this.objectProxy = Proxy.newProxyInstance(
                poolClassLoader,
                objectInterfaces,
                new ObjectReflectHandler(p, this, predicate, methodFilter));
    }

    @Override
    public Object getObjectProxy() throws Exception {
        this.checkClosed();
        return objectProxy;
    }

    private static final class ObjectReflectHandler implements InvocationHandler {
        private final Object raw;
        private final PooledObject p;
        private final PooledObjectProxyHandle handle;
        private final BeeObjectPredicate predicate;
        private final BeeObjectMethodFilter methodFilter;

        ObjectReflectHandler(PooledObject p, PooledObjectProxyHandle handle, BeeObjectPredicate predicate, BeeObjectMethodFilter methodFilter) {
            this.p = p;
            this.raw = p.raw;
            this.handle = handle;
            this.predicate = predicate;
            this.methodFilter = methodFilter;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            handle.checkClosed();
            if (methodFilter != null) methodFilter.doFilter(p.key, method.getName(), method.getParameterTypes(), args);

            try {
                Object v = method.invoke(raw, args);
                p.updateAccessTime();
                return v;
            } catch (Exception e) {
                if (predicate != null && isNotBlank(predicate.evictTest(e)))
                    p.abortSelf(DESC_RM_BAD);

                throw e;
            }
        }
    }
}
