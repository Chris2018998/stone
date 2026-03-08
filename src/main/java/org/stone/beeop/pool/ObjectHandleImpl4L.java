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

import org.stone.beeop.BeeMethodLog;
import org.stone.beeop.BeeObjectPredicate;

import static org.stone.beeop.BeeMethodLog.Type_Object_Log;

/**
 * object Handle implement to support log cache
 *
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectHandleImpl4L<K, V> extends ObjectHandleImpl<K, V> {
    //collects method execution logs
    private final ObjectKeyCategoryPool<K, V> pool;

    ObjectHandleImpl4L(PooledObject<K, V> p, BeeObjectPredicate predicate) {
        super(p, predicate);
        this.pool = p.pool;
    }

    //Override call method
    public Object call(String methodName, Class<?>[] types, Object[] params) throws Throwable {
        checkClosed();

        //configured list is null or method name in configured list
        if (objectMethodNameList == null || objectMethodNameList.contains(methodName)) {
            //create a method call log
            BeeMethodLog<K> log = pool.beforeCall(System.currentTimeMillis(), p.key, Type_Object_Log, "ObjectHandleImpl4L.call", params);

            try {
                Object v = p.callMethod(methodName, types, params);
                long callEndTime = System.currentTimeMillis();
                p.updateAccessTime(callEndTime);//update last accessed time
                pool.afterCall(callEndTime, v, log);//fill successful result to log
                return v;
            } catch (Throwable e) {
                pool.afterCall(System.currentTimeMillis(), e, log);//fill failure cause to log
                throw e;
            }
        } else {
            return p.callMethod(methodName, types, params);
        }
    }
}
