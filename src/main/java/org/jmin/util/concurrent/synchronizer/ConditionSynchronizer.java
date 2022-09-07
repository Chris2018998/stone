/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer;

import org.jmin.util.concurrent.synchronizer.chain.SynchronizeNodeChain;

/**
 * @author Chris Liao
 * @version 1.0
 */
public interface ConditionSynchronizer extends SynchronizeNodeChain {

    /**
     * if true,then wakeup all waiters
     * <p>
     * CountDownLatch,CyclicBarrier
     */
    boolean hitCondition();

}
