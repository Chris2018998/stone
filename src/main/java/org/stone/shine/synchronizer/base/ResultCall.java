/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine.synchronizer.base;

/**
 * Object work as a plugin class to @link{ResultWaitPool}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ResultCall {

    //do some thing
    Object call(Object arg) throws Exception;
}
