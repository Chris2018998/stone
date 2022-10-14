/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.ThreadWaitPool;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * get notification,message,command or other
 *
 * @author Chris Liao
 * @version 1.0
 */

public abstract class SynThreadWaitPool implements ThreadWaitPool {

    //ignore interruption
    public Object getUninterruptibly(Object arg) {
        try {
            ThreadParkerFactory.ThreadParker parker = ThreadParkerFactory.create(0, false);
            parker.setAutoClearInterruptedInd(true);
            return get(arg, parker);
        } catch (TimeoutException e) {
            //in fact,TimeoutException never be thrown out here
            return null;
        } catch (InterruptedException e) {
            //in fact,InterruptedException never be thrown out here
            return null;
        }
    }

    //throws InterruptedException if the current thread is interrupted while getting
    public Object get(Object arg) throws InterruptedException {
        try {
            return get(arg, ThreadParkerFactory.create(0, false));
        } catch (TimeoutException e) {
            //in fact,TimeoutException never be thrown out here
            return null;
        }
    }

    //if got failed,then causes the current thread to wait until interrupted or timeout
    public Object get(Object arg, Date utilDate) throws InterruptedException, TimeoutException {
        return get(arg, ThreadParkerFactory.create(utilDate.getTime(), true));
    }

    //if got failed,then causes the current thread to wait until interrupted or timeout
    public Object get(Object arg, long timeOut, TimeUnit unit) throws InterruptedException, TimeoutException {
        return get(arg, ThreadParkerFactory.create(unit.toNanos(timeOut), false));
    }

    //need implement in sub class
    public abstract Object get(Object type, ThreadParkerFactory.ThreadParker parker) throws InterruptedException, TimeoutException;

}
