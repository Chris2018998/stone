/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import static java.lang.Boolean.TRUE;

/**
 * Work as a plugin of {@code ResultWaitPool},two abstract methods need be implemented in its sub classes
 * <p>
 * Method {@link #call} :acquireWithType resource(lock,permit,and so on)
 * <p>
 * Method {@link #tryRelease}:release acquired resource
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class AcquireAction implements ResultCall {

    //try release
    public abstract boolean tryRelease(int size);

    public final boolean tryAcquire(int size) {
        try {
            return TRUE.equals(call(size));
        } catch (Throwable e) {
            return false;
        }
    }
}
