/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

/**
 * Pooled object transfer policy interface
 *
 * @author Chris Liao
 * @version 1.0
 */
interface ObjectTransferPolicy {
    int getStateCodeOnRelease();

    boolean tryCatch(PooledObject p);
}