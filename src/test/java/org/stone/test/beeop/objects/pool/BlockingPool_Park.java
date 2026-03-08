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

import org.stone.beeop.exception.BeeObjectSourceCreatedException;
import org.stone.beeop.exception.BeeObjectSourcePoolInstantiatedException;

import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class BlockingPool_Park extends BookPool {

    public BlockingPool_Park() {
        LockSupport.park();
        if (Thread.interrupted())
            throw new BeeObjectSourceCreatedException(new BeeObjectSourcePoolInstantiatedException("Interruption occurred during pool being instantiated", new InterruptedException()));
    }
}