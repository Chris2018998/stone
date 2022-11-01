/*
 * Copyright(C) Chris2018998,All rights reserved
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.pool;

/**
 * Pool Connection borrower
 *
 * @author Chris Liao
 * @version 1.0
 */
final class Borrower {
    final Thread thread = Thread.currentThread();
    volatile Object state;
    PooledConnection lastUsed;
}