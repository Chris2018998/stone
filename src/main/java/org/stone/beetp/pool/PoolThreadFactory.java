/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp.pool;

import org.stone.beetp.TaskPoolThreadFactory;

/**
 * Task worker thread factory
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class PoolThreadFactory implements TaskPoolThreadFactory {

    public Thread newThread(Runnable r) {
        return new Thread(r, "beetp-thread");
    }
}