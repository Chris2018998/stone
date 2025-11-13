/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools.extension;

import java.util.List;

/**
 * Interruptable interface.
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeInterruptable {

    /**
     * Attempt to interrupt waiting threads.
     *
     * @return interrupted threads
     */
    List<Thread> interruptQueuedWaitThreads();

}
