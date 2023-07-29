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

public final class SyncWorkConfig<E> implements java.io.Serializable {

    //************************************************node configuration**********************************************//
    //node value
    private E nodeValue;
    //node type
    private Object nodeType;
    //node type
    private Object nodeState;
    //node object
    private SyncNode<E> syncNode;

    //***********************************************block configuration**********************************************//
    //block impl by LockSupport.park Methods
    private SyncParkSupport parkSupport;
    //if interrupted then throws InterruptionException when this ind is true
    private boolean supportInterrupted = true;

    //***********************************************wakeup configuration*********************************************//
    //wakeup next waiter on action success;
    private boolean wakeupNextOnSuccess;
    //wakeup next waiter with same type
    private boolean wakeupSameTypeOnSuccess;
    //wakeup next waiter on action failure(timeout,interrupted)
    private boolean wakeupNextOnFailure = true;
    //wakeup next waiter with same type on
    private boolean wakeupSameTypeOnFailure;

    //****************************************************************************************************************//
    //                                              1: constructors                                                   //
    //****************************************************************************************************************//
    public SyncWorkConfig(long blockTime, TimeUnit timeUnit, Object blockObject, boolean isUtilBlock) {
        if (blockTime > 0) {
            if (timeUnit == null) throw new IllegalArgumentException("time unit can't be null");
            if (isUtilBlock) {
                long blockTimeMillis = timeUnit.toMillis(blockTime);
                this.parkSupport = blockObject == null ? new SyncParkSupport.UtilMillsBlockSupport1(blockTimeMillis) :
                        new SyncParkSupport.UtilMillsBlockSupport2(blockTimeMillis, blockObject);
            } else {
                long blockNanos = timeUnit.toNanos(blockTime);
                this.parkSupport = blockObject == null ? new SyncParkSupport.NanoSecondsBlockSupport(blockNanos) :
                        new SyncParkSupport.NanoSecondsBlockSupport2(blockNanos, blockObject);
            }
        } else {
            this.parkSupport = blockObject == null ? new SyncParkSupport() :
                    new SyncParkSupport.ThreadBlockSupport2(blockObject);
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

    public final SyncNode getSyncNode() {
        if (syncNode != null) return syncNode;
        this.syncNode = new SyncNode<>(nodeState, nodeType, nodeValue);
        this.syncNode.setOwnerThread();
        return this.syncNode;
    }

    //****************************************************************************************************************//
    //                                              4: block configuration(3)                                         //
    //****************************************************************************************************************//
    public SyncParkSupport getParkSupport() {
        return parkSupport;
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
    public boolean isWakeupNextOnSuccess() {
        return wakeupNextOnSuccess;
    }

    public void setWakeupNextOnSuccess(boolean wakeupNextOnSuccess) {
        this.wakeupNextOnSuccess = wakeupNextOnSuccess;
    }

    public boolean isWakeupSameTypeOnSuccess() {
        return wakeupSameTypeOnSuccess;
    }

    public void setWakeupSameTypeOnSuccess(boolean wakeupSameTypeOnSuccess) {
        this.wakeupSameTypeOnSuccess = wakeupSameTypeOnSuccess;
    }

    public boolean isWakeupNextOnFailure() {
        return wakeupNextOnFailure;
    }

    public void setWakeupNextOnFailure(boolean wakeupNextOnFailure) {
        this.wakeupNextOnFailure = wakeupNextOnFailure;
    }

    public boolean isWakeupSameTypeOnFailure() {
        return wakeupSameTypeOnFailure;
    }

    public void setWakeupSameTypeOnFailure(boolean wakeupSameTypeOnFailure) {
        this.wakeupSameTypeOnFailure = wakeupSameTypeOnFailure;
    }

    //****************************************************************************************************************//
    //                                              5: SyncWorkConfig reset                                        //
    //****************************************************************************************************************//
    public final void reset() {
        this.nodeValue = null;
        this.nodeType = null;
        this.nodeState = null;
        this.syncNode = null;
        this.parkSupport.reset();
        this.supportInterrupted = true;

        this.wakeupNextOnFailure = true;
        this.wakeupSameTypeOnFailure = false;
        this.wakeupNextOnSuccess = false;
        this.wakeupSameTypeOnSuccess = false;
    }
}
