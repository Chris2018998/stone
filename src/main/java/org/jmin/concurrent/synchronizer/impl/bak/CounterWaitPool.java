/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer.impl.bak;

/**
 * @author Chris Liao
 * @version 1.0
 * <p>
 * for(CountDownLatch2,CyclicBarrier2)
 */
public abstract class CounterWaitPool extends SynchronizeWaitChain {

    /**
     * if true,then wakeup all waiters
     */
    abstract boolean hitCount();

}
