/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer.impl;

import org.jmin.util.concurrent.synchronizer.SynchronizeNodeChain;

/**
 * @author Chris Liao
 * @version 1.0
 */
public interface CountWaitPool extends SynchronizeNodeChain {

    /**
     * if true,then wakeup all waiters
     * <p>
     * CountDownLatch,CyclicBarrier
     */
    boolean hitCount();

}
