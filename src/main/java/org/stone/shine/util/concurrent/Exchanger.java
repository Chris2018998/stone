/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.TransferWaitPool;
import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.stone.shine.util.concurrent.synchronizer.TransferWaitPool.Node_Type_Data;

/**
 * Exchanger,a synchronization impl by wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Exchanger<E> {
    private final TransferWaitPool<E> waitPool = new TransferWaitPool<>();

    public E exchange(E x) throws InterruptedException {
        SyncVisitConfig<E> config = new SyncVisitConfig<>();
        config.setNodeInitInfo(Node_Type_Data, x);
        SyncNode<E> pairNode = waitPool.transfer(config, Node_Type_Data);
        return pairNode.getValue();
    }

    public E exchange(E x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        SyncVisitConfig<E> config = new SyncVisitConfig<>(timeout, unit);
        config.setNodeInitInfo(Node_Type_Data, x);
        SyncNode<E> pairNode = waitPool.transfer(config, Node_Type_Data);
        if (config.getParkSupport().isTimeout()) throw new TimeoutException();
        return pairNode.getValue();
    }
}
