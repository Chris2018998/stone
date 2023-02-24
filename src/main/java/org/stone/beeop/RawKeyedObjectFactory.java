/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop;

/**
 * keyed Object factory interface
 *
 * @author Chris
 * @version 1.0
 */
public interface RawKeyedObjectFactory<K, E> {

    //create object instance
    E create(K k) throws Exception;

    //set default values to raw object on initialization
    void setDefault(K k, E obj) throws Exception;

    //reset some changed properties in raw object on returning
    void reset(K k, E obj) throws Exception;

    //test raw object valid
    boolean isValid(K k, E obj, int timeout);

    //destroy raw object on removed from pool
    void destroy(K k, E obj);

}
