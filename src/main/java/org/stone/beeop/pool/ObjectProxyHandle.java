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

import org.stone.beeop.pool.exception.ObjectException;

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
    private boolean proxyCreated;

    ObjectProxyHandle(PooledObject p) {
        super(p);
    }

    public Object getObjectProxy() throws Exception {
        if (isClosed) throw new ObjectException("No operations allowed after object handle closed");
        if (proxyCreated) return objectProxy;

        synchronized (this) {
            if (!proxyCreated) {
                try {
                    objectProxy = Proxy.newProxyInstance(
                            PoolClassLoader,
                            p.objectInterfaces,
                            new ObjectReflectHandler(p, this));
                } finally {
                    proxyCreated = true;
                }
            }
        }
        return objectProxy;
    }
}
