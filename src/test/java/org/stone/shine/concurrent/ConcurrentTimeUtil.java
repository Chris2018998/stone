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

/**
 * base Test thread
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ConcurrentTimeUtil {
    public static final long Global_Timeout = 2;
    public static final TimeUnit Global_TimeUnit = TimeUnit.SECONDS;
    public static final long Global_TimeoutNanos = Global_TimeUnit.toNanos(Global_Timeout+1);

    public static final long ParkDelayNanos = TimeUnit.SECONDS.toNanos(1);

    public static long getConcurrentNanoSeconds(int seconds) {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
    }
}
