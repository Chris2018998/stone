/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * common util
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CommonUtil {
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final long spinForTimeoutThreshold = 1023L;
    public static final int maxTimedSpins = (NCPU < 2) ? 0 : 32;

    public static String trimString(String value) {
        return value == null ? null : value.trim();
    }

    public static boolean objectEquals(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static boolean isBlank(String str) {
        if (str == null) return true;
        for (int i = 0, l = str.length(); i < l; ++i) {
            if (!Character.isWhitespace((int) str.charAt(i)))
                return false;
        }
        return true;
    }

    //xor
    public static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        return probe;
    }

    public static void interruptWaitersOnLock(ReentrantLock lock) throws Exception {
        //1: get syn field value from lock
        Field syncField = lock.getClass().getDeclaredField("sync");
        syncField.setAccessible(true);
        Object sync = syncField.get(lock);

        //2: get reflection methods about lock threads
        Method ownerThreadsMethod = AbstractOwnableSynchronizer.class.getDeclaredMethod("getExclusiveOwnerThread");
        Method waitingThreadsMethod = AbstractQueuedSynchronizer.class.getDeclaredMethod("getExclusiveQueuedThreads");
        ownerThreadsMethod.setAccessible(true);
        waitingThreadsMethod.setAccessible(true);

        /*
         * 3:interrupt threads on lock
         * if interruption flag updated after lock acquiring success, and interrupted exception thrown from AQS method at next acquiring
         * should add a cas <method>cancelAcquire(Thread thread)</method>to do interruption in AQS?
         */
        Object[] parameters = new Object[0];
        Thread ownerThread = (Thread) ownerThreadsMethod.invoke(sync, parameters);
        if (ownerThread != null) ownerThread.interrupt();//owner thread maybe stuck on socket
        Collection<Thread> waitingThreads = (Collection<Thread>) waitingThreadsMethod.invoke(sync, parameters);//waiting for lock
        if (waitingThreads != null) {
            for (Thread thread : waitingThreads) {
                thread.interrupt();
            }
        }
    }
}
