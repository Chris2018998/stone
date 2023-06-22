/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.tools.atomic;

import java.lang.reflect.Field;

/**
 * Unsafe Adaptor
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface UnsafeAdaptor {

    //****************************************************************************************************************//
    //                                            field offset                                                        //
    //****************************************************************************************************************//

    long objectFieldOffset(Field field);

    long staticFieldOffset(Field field);

    Object staticFieldBase(Field field);

    //****************************************************************************************************************//
    //                                            volatile(int)                                                       //
    //****************************************************************************************************************//
    int getIntVolatile(Object object, long offset);

    void putIntVolatile(Object object, long offset, int update);

    void putOrderedInt(Object object, long offset, int update);

    boolean compareAndSwapInt(Object object, long offset, int expect, int update);

    //****************************************************************************************************************//
    //                                            volatile(long)                                                      //
    //****************************************************************************************************************//
    long getLongVolatile(Object object, long offset);

    void putOrderedLong(Object object, long offset, long update);

    void putLongVolatile(Object object, long offset, long update);

    boolean compareAndSwapLong(Object object, long offset, long expect, long update);

    //****************************************************************************************************************//
    //                                            volatile(Object)                                                    //
    //****************************************************************************************************************//
    Object getObjectVolatile(Object object, long offset);

    void putOrderedObject(Object object, long offset, Object update);

    void putObjectVolatile(Object object, long offset, Object update);

    boolean compareAndSwapObject(Object object, long offset, Object expect, Object update);
}
