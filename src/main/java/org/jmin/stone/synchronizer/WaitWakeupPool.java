/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer;

import java.util.concurrent.TimeUnit;

/**
 * Thread wait pool util wake-up,which is the simplest synchronize way
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface WaitWakeupPool {

    //Wakeup all blocking threads in pool
    void wakeupWaiting();

    /**
     * Condition test method,for example(NEW CountDownLatch Implementation)
     * <code>
     * public boolean testCondition(){
     * return countAtomicInteger.get()==0;
     * }
     * </code>
     *
     * @return true, All block threads should be wakeup and leave pool
     */
    boolean testCondition();

    /**
     * When true,then can reset condition to initialization state,for example(NEW CountDownLatch Implementation)
     * public void resetCondition(){
     * if(countAtomicInteger.get()==0){ countAtomicInteger.set(10); }
     * }
     */
    void resetCondition();

    /**
     * Block current thread,
     * Before blocking,need execute<method>testCondition</method>,if true then return immediately,
     * false,blocking util wakeup by other thread called <method>wakeupWaiting</method> or interrupted.
     *
     * @throws InterruptedException interrupted during blocking
     */
    void await() throws InterruptedException;

    /**
     * Block current thread,
     * Before blocking,need execute<method>testCondition</method>,if true then return immediately,
     * false,blocking util wakeup by other thread called <method>wakeupWaiting</method> or interrupted or timeout
     *
     * @return true, the test condition has been true,otherwise return false
     * @throws InterruptedException interrupted during blocking
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;
}
