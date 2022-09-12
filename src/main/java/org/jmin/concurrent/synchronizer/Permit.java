/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer;

/**
 * a type pooled resource
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface Permit {

    int getHoldCount();

    Thread getHoldThread();

}
