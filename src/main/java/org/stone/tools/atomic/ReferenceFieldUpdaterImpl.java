/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.atomic;

import jdk.internal.misc.Unsafe;
import org.stone.tools.UnsafeHolder;
import org.stone.tools.exception.ReflectionOperationException;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Atomic Reference Field Updater Implementation(Don't use in other place)
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
    private final static Unsafe unsafe = UnsafeHolder.getUnsafe();

    private final long offset;
    private final Class<V> fieldType;

    private ReferenceFieldUpdaterImpl(long offset, Class<V> fieldType) {
        this.offset = offset;
        this.fieldType = fieldType;
    }

    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
        try {
            return new ReferenceFieldUpdaterImpl<>(unsafe.objectFieldOffset(tclass.getDeclaredField(fieldName)), vclass);
        } catch (NoSuchFieldException e) {
            throw new ReflectionOperationException(e);
        } catch (SecurityException e) {
            throw e;
        } catch (Throwable e) {
            return AtomicReferenceFieldUpdater.newUpdater(tclass, vclass, fieldName);
        }
    }


    public boolean compareAndSet(T bean, V expect, V update) {
        return unsafe.compareAndSetReference(bean, this.offset, expect, update);
    }

    @Override
    public boolean weakCompareAndSet(T bean, V expect, V update) {
        return unsafe.compareAndSetReference(bean, this.offset, expect, update);
    }

    @Override
    public void set(T bean, V newValue) {
        unsafe.putReferenceVolatile(bean, this.offset, newValue);
    }

    @Override
    public void lazySet(T bean, V newValue) {
        unsafe.putReferenceRelease(bean, this.offset, newValue);
    }

    public V get(T bean) {
        return fieldType.cast(unsafe.getReferenceVolatile(bean, this.offset));
    }
}