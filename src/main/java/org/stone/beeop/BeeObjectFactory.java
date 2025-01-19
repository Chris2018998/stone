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
public interface BeeObjectFactory {

    //Returns default key
    Object getDefaultKey();

    //Creates an object with given key
    Object create(Object key) throws Exception;

    //Set default on a new object.
    void setDefault(Object key, Object obj) throws Exception;

    //Reset default on a released object
    void reset(Object key, Object obj) throws Exception;

    //Alive test on borrowed object
    boolean isValid(Object key, Object obj, int timeout) throws Exception;

    //Destroy object
    void destroy(Object key, Object obj) throws Exception;
}
