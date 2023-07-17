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

/**
 * Thread wait control parameter for wait pool,which is once-only use object
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadBlockConfig<E> implements java.io.Serializable {
    //************************************************node configuration**********************************************//
    //node value
    private E nodeValue;
    //node type
    private Object nodeType;
    //node type
    private Object nodeState;
    //node object
    private CasNode casNode;

    //***********************************************block configuration**********************************************//
    //block impl by LockSupport.park Methods
    private ThreadBlockSupport blockSupport;
    //if interrupted then throws InterruptionException when this ind is true
    private boolean supportInterrupted = true;

    //***********************************************wakeup configuration*********************************************//
    //wakeup one waiter on action failure;
    private boolean wakeupOneOnFailure = true;
    //wakeup one waiter with same type on
    private boolean wakeupSameTypeOnFailure;
    //wakeup one waiter on action success;
    private boolean wakeupOtherOnSuccess;
    //wakeup one waiter with same type
    private boolean wakeupSameTypeOnSuccess;

    //****************************************************************************************************************//
    //                                              1: constructors                                                   //
    //****************************************************************************************************************//
    public ThreadBlockConfig(long blockTime, TimeUnit timeUnit, boolean isUtilBlock, Object blockObject) {
        if (blockTime > 0) {
            if (timeUnit == null) throw new IllegalArgumentException("time unit can't be null");
            if (isUtilBlock) {
                long blockTimeMillis = timeUnit.toMillis(blockTime);
                this.blockSupport = blockObject == null ? new ThreadBlockSupport.UtilMillsBlockSupport1(blockTimeMillis) :
                        new ThreadBlockSupport.UtilMillsBlockSupport2(blockTimeMillis, blockObject);
            } else {
                long blockNanos = timeUnit.toNanos(blockTime);
                this.blockSupport = blockObject == null ? new ThreadBlockSupport.NanoSecondsBlockSupport(blockNanos) :
                        new ThreadBlockSupport.NanoSecondsBlockSupport2(blockNanos, blockObject);
            }
        } else {
            this.blockSupport = blockObject == null ? new ThreadBlockSupport() :
                    new ThreadBlockSupport.ThreadBlockSupport2(blockObject);
        }
    }

    //*************************************************node configuration*********************************************//
    public E getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(E nodeValue) {
        this.nodeValue = nodeValue;
    }

    public Object getNodeType() {
        return nodeType;
    }

    public void setNodeType(Object nodeType) {
        this.nodeType = nodeType;
    }

    public Object getNodeState() {
        return nodeState;
    }

    public void setNodeState(Object nodeState) {
        this.nodeState = nodeState;
    }

    public final CasNode getCasNode() {
        if (casNode != null) return casNode;
        return this.casNode = new CasNode<>(nodeType, nodeValue);
    }

    //****************************************************************************************************************//
    //                                              4: block configuration(3)                                         //
    //****************************************************************************************************************//
    public ThreadBlockSupport getBlockSupport() {
        return blockSupport;
    }

    public boolean isSupportInterrupted() {
        return supportInterrupted;
    }

    public void setSupportInterrupted(boolean supportInterrupted) {
        this.supportInterrupted = supportInterrupted;
    }

    //****************************************************************************************************************//
    //                                              4: wakeup configuration(8)                                        //
    //****************************************************************************************************************//
    public boolean isWakeupOneOnFailure() {
        return wakeupOneOnFailure;
    }

    public void setWakeupOneOnFailure(boolean wakeupOneOnFailure) {
        this.wakeupOneOnFailure = wakeupOneOnFailure;
    }

    public boolean isWakeupSameTypeOnFailure() {
        return wakeupSameTypeOnFailure;
    }

    public void setWakeupSameTypeOnFailure(boolean wakeupSameTypeOnFailure) {
        this.wakeupSameTypeOnFailure = wakeupSameTypeOnFailure;
    }

    public boolean isWakeupOtherOnSuccess() {
        return wakeupOtherOnSuccess;
    }

    public void setWakeupOtherOnSuccess(boolean wakeupOtherOnSuccess) {
        this.wakeupOtherOnSuccess = wakeupOtherOnSuccess;
    }

    public boolean isWakeupSameTypeOnSuccess() {
        return wakeupSameTypeOnSuccess;
    }

    public void setWakeupSameTypeOnSuccess(boolean wakeupSameTypeOnSuccess) {
        this.wakeupSameTypeOnSuccess = wakeupSameTypeOnSuccess;
    }

    //****************************************************************************************************************//
    //                                              5: ThreadBlockConfig reset                                        //
    //****************************************************************************************************************//
    public final void reset() {
        this.blockSupport.reset();
        this.nodeType = null;
        this.nodeValue = null;
        this.casNode = null;
    }
}
