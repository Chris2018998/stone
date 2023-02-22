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

import org.stone.beeop.RawObjectFactory;

import java.lang.reflect.Constructor;

/**
 * Object instance factory by class
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class SimpleObjectFactory<E> implements RawObjectFactory<E> {
    private final Constructor<E> constructor;

    public SimpleObjectFactory(Constructor<E> constructor) {
        this.constructor = constructor;
    }

    //create object instance
    public E create(Object key) throws Exception {
        return this.constructor.newInstance();
    }

    //set default values
    public void setDefault(E obj) {
        //do nothing
    }

    //set default values
    public void reset(E obj) {
        //do nothing
    }

    //test object
    public boolean isValid(E obj, int timeout) {
        return true;
    }

    //destroy  object
    public void destroy(E obj) {
        //do nothing
    }
}
