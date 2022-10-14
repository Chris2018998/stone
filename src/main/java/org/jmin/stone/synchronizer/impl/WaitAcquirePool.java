/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import java.util.concurrent.TimeoutException;

/**
 * get notification,message,command or other
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class WaitAcquirePool extends SynThreadWaitPool {

    public abstract int tryAcquire(Object arg);

    public Object get(Object arg, ThreadParkerFactory.ThreadParker parker) throws InterruptedException, TimeoutException {
        //@todo
        return null;
    }
}

