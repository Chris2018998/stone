/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl.bak;

/**
 * pooled resource
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface Permit {

    int getState();

    int getHoldCount();

    Thread getHoldThread();

}
