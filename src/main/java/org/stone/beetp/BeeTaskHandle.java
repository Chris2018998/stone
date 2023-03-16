/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beetp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task handle
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeTaskHandle {

    boolean isDone();

    boolean isInQueue();

    boolean isInRunning();

    boolean isCanceled();

    boolean tryCancel(boolean mayInterrupted);

    Object get() throws BeeTaskException, InterruptedException;

    Object get(long timeout, TimeUnit unit) throws BeeTaskException, TimeoutException, InterruptedException;

}
