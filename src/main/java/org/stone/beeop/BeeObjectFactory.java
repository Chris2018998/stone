/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * Object factory interface.
 *
 * @author Chris
 * @version 1.0
 */
public interface BeeObjectFactory<K, V> {

    //Returns default key
    K getDefaultKey();

    //Creates an object with given key
    V create(K key) throws Exception;

    //Set default on a new object.
    void setDefault(K key, V obj) throws Exception;

    //Reset default on a released object
    void reset(K key, V obj) throws Exception;

    //Alive test on borrowed object
    boolean isValid(K key, V obj, int timeout) throws Exception;

    //Destroy object
    void destroy(K key, V obj) throws Exception;
}
