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
 * Object factory interface
 *
 * @author Chris
 * @version 1.0
 */
public interface BeeObjectFactory {

    //returns default key to be pooled
    Object getDefaultKey();

    //creates an object to pool with specified key
    Object create(Object key) throws Exception;

    //set default to a pooled object
    void setDefault(Object key, Object obj) throws Exception;

    //reset dirty properties of given object to default
    void reset(Object key, Object obj) throws Exception;

    //executes alive test on an object
    boolean isValid(Object key, Object obj, int timeout) throws Exception;

    //destroy an object
    void destroy(Object key, Object obj) throws Exception;
}
