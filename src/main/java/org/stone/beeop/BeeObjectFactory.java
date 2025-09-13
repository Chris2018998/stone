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
 * @param <K> is pooled key
 * @param <V> is pooled object type
 * @author Chris
 * @version 1.0
 */
public interface BeeObjectFactory<K, V> {

    //Returns key of default category type
    K getDefaultKey();

    //Creates object instance with given key
    V create(K key) throws Exception;

    //set default value to properties of object after creation
    void setDefault(K key, V obj) throws Exception;

    //reset default value to properties of object before released to pool
    void reset(K key, V obj) throws Exception;

    //test object alive before it taken out from pool to a borrower
    boolean isValid(K key, V obj, int timeout) throws Exception;

    //destroy an object when pool clean and pool close
    void destroy(K key, V obj) throws Exception;
}
