/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Thread wait control parameter for wait pool,which is once-only use object
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadWaitConfig<E> implements java.io.Serializable {
    //************************************************A: wait node config*********************************************//
    //node type
    private Object nodeType;
    //node value
    private E nodeValue;
    //node object
    private CasNode threadNode;
    //need add into queue of wait pool
    private boolean outsideOfWaitPool = true;
    //***********************************************B: wait time config**********************************************//
    //wait time value(Nanoseconds or Milliseconds)
    private long maxWaitTime;
    //wait util deadline(using in LockSupport.parkUtil)
    private boolean isMilliseconds;
    //time block object(using in LockSupport.park,LockSupport.parkNanos)
    private Object waitBlocker;
    //park support
    private ThreadParkSupport parkSupport;
    //***********************************************C: IE config*****************************************************//
    //indicator of throw interruptException when interrupted
    private boolean throwsIE = true;
    //transfer got signal to other when got transfer signal but interrupted
    private boolean transferSignalOnIE = true;

    //****************************************************************************************************************//
    //                                              1: constructors methods(5)                                        //
    //****************************************************************************************************************//
    public ThreadWaitConfig() {
    }

    public ThreadWaitConfig(Object nodeType) {
        this.nodeType = nodeType;
    }

    public ThreadWaitConfig(Date waitDeadline) {
        this.setWaitDeadline(waitDeadline);
    }

    public ThreadWaitConfig(long maxWaitTime, TimeUnit waitTimeUnit) {
        this.setMaxWaitTime(maxWaitTime, waitTimeUnit, null);
    }

    public ThreadWaitConfig(long maxWaitTime, TimeUnit waitTimeUnit, Object nodeType) {
        this.setMaxWaitTime(maxWaitTime, waitTimeUnit, null);
        this.nodeType = nodeType;
    }

    //****************************************************************************************************************//
    //                                              2: node methods(5)                                                //
    //****************************************************************************************************************//
    public final void setNodeValue(Object nodeType, E nodeValue) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
    }

    public final CasNode getThreadNode() {
        if (threadNode != null) return threadNode;
        return this.threadNode = new CasNode<>(nodeType, nodeValue);
    }

    public final void setThreadNode(CasNode threadNode) {
        this.threadNode = threadNode;
    }

    public final boolean isOutsideOfWaitPool() {
        return outsideOfWaitPool;
    }

    public final void setOutsideOfWaitPool(boolean outsideOfWaitPool) {
        this.outsideOfWaitPool = outsideOfWaitPool;
    }

    //****************************************************************************************************************//
    //                                              3: wait time config(5)                                            //
    //****************************************************************************************************************//
    public final void setMaxWaitTime(long maxWaitTime, TimeUnit waitTimeUnit) {
        this.setMaxWaitTime(maxWaitTime, waitTimeUnit, null);
    }

    public final void setMaxWaitTime(long maxWaitTime, TimeUnit waitTimeUnit, Object waitBlocker) {
        if (waitTimeUnit == null) throw new IllegalArgumentException("timeUnit can't be null");
        this.maxWaitTime = waitTimeUnit.toNanos(maxWaitTime);
        this.waitBlocker = waitBlocker;
    }

    public final void setWaitDeadline(Date waitDeadline) {
        this.setWaitDeadline(waitDeadline, null);
    }

    public final void setWaitDeadline(Date waitDeadline, Object waitBlocker) {
        if (waitDeadline == null) throw new IllegalArgumentException("deadline can't be null");
        this.maxWaitTime = waitDeadline.getTime();
        this.waitBlocker = waitBlocker;
        this.isMilliseconds = true;
    }

    public final ThreadParkSupport getThreadParkSupport() {
        if (parkSupport != null) return parkSupport;

        if (waitBlocker != null)
            return parkSupport = ThreadParkSupport.create(maxWaitTime, isMilliseconds, waitBlocker);

        return parkSupport = ThreadParkSupport.create(maxWaitTime, isMilliseconds);
    }

    //****************************************************************************************************************//
    //                                              4: Interrupt Config(4)                                            //
    //****************************************************************************************************************//
    public final boolean isThrowsIE() {
        return throwsIE;
    }

    public final void setThrowsIE(boolean throwsIE) {
        this.throwsIE = throwsIE;
    }

    public final boolean isTransferSignalOnIE() {
        return transferSignalOnIE;
    }

    public final void setTransferSignalOnIE(boolean transferSignalOnIE) {
        this.transferSignalOnIE = transferSignalOnIE;
    }
}
