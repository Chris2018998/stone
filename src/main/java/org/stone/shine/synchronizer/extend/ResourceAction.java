/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.extend;

import org.stone.shine.synchronizer.base.ResultCall;

import java.util.Objects;

/**
 * Work as a plugin of {@code ResourceWaitPool},two abstract methods need be implemented in its sub classes
 * <p>
 * Method {@link #call} :acquire resource(for example; lock,permit)
 * <p>
 * Method {@link #tryRelease}:release acquired resource
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class ResourceAction implements ResultCall {

    //try release
    abstract boolean tryRelease(int size);

    //try to acquire lock or permit
    public boolean tryAcquire(int size) {
        try {
            return Objects.equals(this.call(size), true);
        } catch (Exception e) {
            return false;
        }
    }
}
