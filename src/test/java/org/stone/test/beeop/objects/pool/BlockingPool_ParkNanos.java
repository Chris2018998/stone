/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects.pool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class BlockingPool_ParkNanos extends BookPool {
    public BlockingPool_ParkNanos() {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1L));
        if (Thread.interrupted()) if (Thread.interrupted()) throw new RuntimeInterruptedException("Internal error");
    }
}
