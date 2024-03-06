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
public interface RawObjectFactory {

    //create a key for default pooled objects
    Object getDefaultKey();

    //create object instance
    Object create(Object key) throws Exception;

    //set default value to keyed object after object creation
    void setDefault(Object key, Object obj) throws Exception;

    //reset some default value to dirty properties
    void reset(Object key, Object obj) throws Exception;

    //alive test on a borrowed object
    boolean isValid(Object key, Object obj, int timeout);

    //destroy a keyed object when object is bad or pool clearing
    void destroy(Object key, Object obj);
}
