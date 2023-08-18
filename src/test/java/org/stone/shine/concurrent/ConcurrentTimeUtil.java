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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * base Test thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConcurrentTimeUtil {
    public static final long ParkNanos = 5L;
    public static final long Wait_Time = 100L;
    public static final TimeUnit Wait_TimeUnit = TimeUnit.MILLISECONDS;

    public static boolean isInWaiting(Thread thread, long parkNanos) {
        for (; ; ) {
            Thread.State curState = thread.getState();
            if (curState == Thread.State.WAITING || curState == Thread.State.TIMED_WAITING) {
                return true;
            } else if (curState == Thread.State.TERMINATED) {
                return false;
            } else {
                LockSupport.parkNanos(parkNanos);
            }
        }
    }
}
