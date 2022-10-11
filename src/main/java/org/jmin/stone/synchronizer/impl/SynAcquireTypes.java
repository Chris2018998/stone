/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

/**
 * Resource hold type,which can set to value field of chain node
 *
 * @author Chris Liao
 * @version 1.0
 */

final class SynAcquireTypes {

    //exclusive hold(default)
    public static final int EXCLUSIVE_HOLD = 1;

    //sharable hold
    public static final int SHARABLE_HOLD = 2;
}
