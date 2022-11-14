/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.semaphore;

import org.stone.shine.synchronizer.permit.Semaphore;

/**
 * Semaphore Simple Test
 *
 * @author Chris Liao
 * @version 1.0
 */
public class SemaphoreSimpleTest {
    public static void main(String[] args) throws Exception {
        Semaphore semaphore = new Semaphore(5);
        semaphore.acquire();
        System.out.println("semaphore available size:" + semaphore.availablePermits());

        semaphore.release();
        System.out.println("semaphore available size:" + semaphore.availablePermits());
    }
}
