/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop;

/**
 * Object instance factory
 *
 * @author Chris
 * @version 1.0
 */
public interface RawObjectFactory {

    //create object instance
    Object create() throws Exception;

    //set default values to raw object on initialization
    void setDefault(Object obj) throws Exception;

    //reset some changed properties in raw object on returning
    void reset(Object obj) throws Exception;

    //test raw object valid
    boolean isValid(Object obj, int timeout);

    //destroy raw object on removed from pool
    void destroy(Object obj);
}
