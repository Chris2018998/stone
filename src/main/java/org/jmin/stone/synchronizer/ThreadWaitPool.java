/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * get notification,message,command or other
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ThreadWaitPool {
    //****************************************************************************************************************//
    //                                          1: get Methods                                                        //
    //****************************************************************************************************************//
    //ignore interruption
    Object getUninterruptibly(Object arg);

    //throws InterruptedException if the current thread is interrupted while getting
    Object get(Object arg) throws InterruptedException;

    //if got failed,then causes the current thread to wait until interrupted or timeout
    Object get(Object arg, Date utilDate) throws InterruptedException, TimeoutException;

    //if got failed,then causes the current thread to wait until interrupted or timeout
    Object get(Object arg, long timeOut, TimeUnit unit) throws InterruptedException, TimeoutException;


    //****************************************************************************************************************//
    //                                          2: monitor Methods                                                    //
    //****************************************************************************************************************//
    int getQueueLength();

    Collection<Thread> getQueuedThreads();

    int getQueueLength(Object arg);

    Collection<Thread> getQueuedThreads(Object arg);
}
