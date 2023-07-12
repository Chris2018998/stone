/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer;

import java.util.concurrent.TimeUnit;

import static org.stone.shine.util.concurrent.synchronizer.CasStaticState.INIT;

/**
 * Thread wait control parameter for wait pool,which is once-only use object
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadSpinConfig<E> implements java.io.Serializable {

    //************************************************A: park config**************************************************//
    private final ThreadSpinParker blocker;

    //************************************************B: wait node config*********************************************//
    //node type
    private Object nodeType;
    //node value
    private E nodeValue;
    //node state
    private Object nodeState = INIT;

    //node object
    private CasNode casNode;
    //need add into queue of wait pool
    private boolean outsideOfWaitPool = true;

    //***********************************************C: IE config*****************************************************//
    //indicator of throw interruptException when interrupted
    private boolean allowThrowsIE = true;
    //transfer got signal to other when got transfer signal but interrupted
    private boolean transferSignalOnIE = true;

    //****************************************************************************************************************//
    //                                              1: constructors methods(6)                                        //
    //****************************************************************************************************************//
    public ThreadSpinConfig() {
        this.blocker = new ThreadSpinParker(allowThrowsIE);
    }

    public ThreadSpinConfig(Object blocker) {
        this.blocker = new ThreadSpinParker.ThreadBlockerParkSupport(blocker, allowThrowsIE);
    }

    public ThreadSpinConfig(long deadlineMs) {
        this.blocker = new ThreadSpinParker.MillisecondsUtilParkSupport(deadlineMs, allowThrowsIE);
    }

    public ThreadSpinConfig(long deadlineMs, Object blocker) {
        this.blocker = new ThreadSpinParker.MillisecondsBlockerUtilParkSupport(deadlineMs, blocker, allowThrowsIE);
    }

    public ThreadSpinConfig(long timeout, TimeUnit unit) {
        if (unit == null) throw new IllegalArgumentException("time unit can't be null");
        this.blocker = new ThreadSpinParker.NanoSecondsParkSupport(unit.toNanos(timeout), allowThrowsIE);
    }

    public ThreadSpinConfig(long timeout, TimeUnit unit, Object blocker) {
        if (unit == null) throw new IllegalArgumentException("time unit can't be null");
        this.blocker = new ThreadSpinParker.NanoSecondsBlockerParkSupport(unit.toNanos(timeout), blocker, allowThrowsIE);
    }

    //****************************************************************************************************************//
    //                                              2: node methods(7)                                                //
    //****************************************************************************************************************//
    public final void setNodeType(Object nodeType) {
        this.nodeType = nodeType;
    }

    public final void setNodeValue(Object nodeType, E nodeValue) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
        this.nodeState = null;
    }

    public final void setNodeValue(Object nodeType, E nodeValue, Object nodeState) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
        this.nodeState = nodeState;
    }

    public final CasNode getCasNode() {
        if (casNode != null) return casNode;
        return this.casNode = new CasNode<>(nodeType, nodeValue, nodeState);
    }

    public final void setCasNode(CasNode casNode) {
        this.casNode = casNode;
    }

    public final ThreadSpinParker getThreadParkSupport() {
        return blocker;
    }

    public final boolean isOutsideOfWaitPool() {
        return outsideOfWaitPool;
    }

    public final void setOutsideOfWaitPool(boolean outsideOfWaitPool) {
        this.outsideOfWaitPool = outsideOfWaitPool;
    }

    //****************************************************************************************************************//
    //                                              4: Interrupt Config(4)                                            //
    //****************************************************************************************************************//
    public final boolean isAllowThrowsIE() {
        return allowThrowsIE;
    }

    public final void allowThrowsIE(boolean allowThrowsIE) {
        this.allowThrowsIE = allowThrowsIE;
    }

    public final boolean isTransferSignalOnIE() {
        return transferSignalOnIE;
    }

    public final void setTransferSignalOnIE(boolean transferSignalOnIE) {
        this.transferSignalOnIE = transferSignalOnIE;
    }

    //****************************************************************************************************************//
    //                                              5: ThreadSpinConfig reset                                         //
    //****************************************************************************************************************//
    public final void reset() {
        this.blocker.reset();
        this.nodeType = null;
        this.nodeValue = null;
        this.casNode = null;
        this.allowThrowsIE = true;
        this.transferSignalOnIE = true;
        this.outsideOfWaitPool = true;
    }
}
