/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.TransferWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Exchanger Impl By Wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Exchanger<E> {
    private TransferWaitPool<E> waitPool = new TransferWaitPool<>();

    public Object exchange(E x) throws InterruptedException {
        SyncVisitConfig config = new SyncVisitConfig();
        return waitPool.transfer(x, config);
    }

    public Object exchange(E x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        SyncVisitConfig config = new SyncVisitConfig(timeout, unit);
        Object v = waitPool.transfer(x, config);
        if (v == null && config.getParkSupport().isTimeout()) throw new TimeoutException();
        return v;
    }
}
