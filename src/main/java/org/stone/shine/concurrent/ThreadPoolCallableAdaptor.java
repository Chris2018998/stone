/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent;

import java.util.concurrent.Callable;

/**
 * Callable Adaptor for runnable
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ThreadPoolCallableAdaptor<V> implements Callable<V> {
    private final V defaultResult;
    private final Runnable runnable;

    public ThreadPoolCallableAdaptor(Runnable runnable) {
        this(runnable, null);
    }

    public ThreadPoolCallableAdaptor(Runnable runnable, V defaultResult) {
        this.runnable = runnable;
        this.defaultResult = defaultResult;
    }

    public V call() throws Exception {
        runnable.run();
        return defaultResult;
    }
}
