/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.resource;

import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * Work as a plugin of {@code ResourceWaitPool},two abstract methods need be implemented in its sub classes
 * <p>
 * Method {@link #call} :acquireWithType resource(lock,permit,and so on)
 * <p>
 * Method {@link #tryRelease}:release acquired resource
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class ResourceAction implements ResultCall {

    //try release
    public abstract boolean tryRelease(int size);

    //try to acquireWithType lock or permit
    public boolean tryAcquire(int size) {
        try {
            return objectEquals(this.call(size), Boolean.TRUE);
        } catch (Throwable e) {
            return false;
        }
    }
}
