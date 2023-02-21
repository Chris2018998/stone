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

import java.util.concurrent.TimeUnit;

import static org.stone.shine.synchronizer.CasStaticState.INIT;

/**
 * Thread wait control parameter for wait pool,which is once-only use object
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadWaitConfig<E> implements java.io.Serializable {

    //************************************************A: park config**************************************************//
    private final ThreadParkSupport parkSupport;

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
    public ThreadWaitConfig() {
        this.parkSupport = new ThreadParkSupport();
    }

    public ThreadWaitConfig(Object blocker) {
        this.parkSupport = new ThreadParkSupport.ThreadBlockerParkSupport(blocker);
    }

    public ThreadWaitConfig(long deadlineMs) {
        this.parkSupport = new ThreadParkSupport.MillisecondsUtilParkSupport(deadlineMs);
    }

    public ThreadWaitConfig(long deadlineMs, Object blocker) {
        this.parkSupport = new ThreadParkSupport.MillisecondsBlockerUtilParkSupport(deadlineMs, blocker);
    }

    public ThreadWaitConfig(long timeout, TimeUnit unit) {
        if (unit == null) throw new IllegalArgumentException("time unit can't be null");
        this.parkSupport = new ThreadParkSupport.NanoSecondsParkSupport(unit.toNanos(timeout));
    }

    public ThreadWaitConfig(long timeout, TimeUnit unit, Object blocker) {
        if (unit == null) throw new IllegalArgumentException("time unit can't be null");
        this.parkSupport = new ThreadParkSupport.NanoSecondsBlockerParkSupport(unit.toNanos(timeout), blocker);
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

    public final ThreadParkSupport getThreadParkSupport() {
        return parkSupport;
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
    //                                              5: ThreadWaitConfig reset                                         //
    //****************************************************************************************************************//
    public final void reset() {
        this.parkSupport.reset();
        this.nodeType = null;
        this.nodeValue = null;
        this.casNode = null;
        this.allowThrowsIE = true;
        this.transferSignalOnIE = true;
        this.outsideOfWaitPool = true;
    }
}
