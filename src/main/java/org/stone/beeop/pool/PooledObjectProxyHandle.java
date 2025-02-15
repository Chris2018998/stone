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
public final class PooledObjectProxyHandle<K, V> extends PooledObjectPlainHandle<K, V> {
    private final V objectProxy;

    PooledObjectProxyHandle(PooledObject<K, V> p, BeeObjectPredicate predicate, ClassLoader poolClassLoader,
                            Class<?>[] objectInterfaces, BeeObjectMethodFilter<K> methodFilter) {
        super(p, predicate);
        this.objectProxy = (V) Proxy.newProxyInstance(
                poolClassLoader,
                objectInterfaces,
                new ObjectReflectHandler(p, this, predicate, methodFilter));
    }

    @Override
    public V getObjectProxy() throws Exception {
        this.checkClosed();
        return objectProxy;
    }

    private static final class ObjectReflectHandler<K, V> implements InvocationHandler {
        private final Object raw;
        private final PooledObject<K, V> p;
        private final PooledObjectProxyHandle<K, V> handle;
        private final BeeObjectPredicate predicate;
        private final BeeObjectMethodFilter<K> methodFilter;

        ObjectReflectHandler(PooledObject<K, V> p, PooledObjectProxyHandle<K, V> handle, BeeObjectPredicate predicate, BeeObjectMethodFilter<K> methodFilter) {
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
