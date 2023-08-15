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
 * synchronization Visit Config
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class SyncVisitConfig<E> implements java.io.Serializable {
    //node value
    private E nodeValue;
    //node type
    private Object nodeType;
    //node init state
    private Object initState;

    //***********************************************spin configuration***********************************************//
    //Park tool implement with class{@code java.util.concurrent.locks.LockSupport}
    private ThreadParkSupport parkSupport;
    //indicator:true,throws InterruptedException when waiting interrupted
    private boolean allowInterruption = true;

    //***********************************************wakeup configuration*********************************************//
    //similar to AQS SHARED mode on acquisition success
    private boolean wakeupOneOnSuccess;
    //store some node type
    private Object wakeupNodeTypeOnSuccess;
    //similar to AQS CANCELLED
    private boolean wakeupOneOnFailure = true;
    //store some node type
    private Object wakeupNodeTypeOnFailure;

    //****************************************************************************************************************//
    //                                              1:constructors(4)                                                 //
    //****************************************************************************************************************//
    public SyncVisitConfig() {
        this(0L, null, null, false);
    }

    public SyncVisitConfig(long parkTime, TimeUnit timeUnit) {
        this(parkTime, timeUnit, null, false);
    }

    public SyncVisitConfig(long parkTime, TimeUnit timeUnit, boolean isUtilTime) {
        this(parkTime, timeUnit, null, isUtilTime);
    }

    public SyncVisitConfig(long parkTime, TimeUnit timeUnit, Object blockObject, boolean isUtilTime) {
        if (parkTime > 0L) {
            if (isUtilTime) {
                this.parkSupport = blockObject == null ? new ThreadParkSupport.UtilMillsParkSupport1(parkTime) :
                        new ThreadParkSupport.UtilMillsParkSupport2(parkTime, blockObject);
            } else {
                if (timeUnit == null) throw new IllegalArgumentException("Time unit can't be null");
                long blockNanos = timeUnit.toNanos(parkTime);
                this.parkSupport = blockObject == null ? new ThreadParkSupport.NanoSecondsParkSupport(blockNanos) :
                        new ThreadParkSupport.NanoSecondsParkSupport2(blockNanos, blockObject);
            }
        } else {
            this.parkSupport = blockObject == null ? new ThreadParkSupport() :
                    new ThreadParkSupport.ThreadParkSupport2(blockObject);
        }
    }

    //****************************************************************************************************************//
    //                                              2:set node info(5)                                                //
    //****************************************************************************************************************//
    public final void setNodeType(Object nodeType) {
        this.nodeType = nodeType;
    }

    public final void setNodeInitState(Object initState) {
        this.initState = initState;
    }

    public final void setNodeInitInfo(Object type, E value) {
        this.nodeType = type;
        this.nodeValue = value;
    }

    public final void setNodeInitInfo(Object type, E value, Object initState) {
        this.nodeType = type;
        this.nodeValue = value;
        this.initState = initState;
    }

    public final SyncNode<E> getSyncNode() {
        return new SyncNode<>(initState, nodeType, nodeValue);
    }

    //****************************************************************************************************************//
    //                                              3: spin configuration(3)                                          //
    //****************************************************************************************************************//
    public final ThreadParkSupport getParkSupport() {
        return parkSupport;
    }

    public final boolean isAllowInterruption() {
        return allowInterruption;
    }

    public void allowInterruption(boolean allowIndicator) {
        this.allowInterruption = allowIndicator;
    }

    //****************************************************************************************************************//
    //                                              4: wakeup configuration(8)                                        //
    //****************************************************************************************************************//
    public final boolean isWakeupOneOnSuccess() {
        return wakeupOneOnSuccess;
    }

    public final void setWakeupOneOnSuccess(boolean wakeupInd) {
        this.wakeupOneOnSuccess = wakeupInd;
    }

    public final Object getWakeupNodeTypeOnSuccess() {
        return wakeupNodeTypeOnSuccess;
    }

    public final void setWakeupOneOnSuccess(boolean wakeupInd, Object nodeType) {
        this.wakeupOneOnSuccess = wakeupInd;
        this.wakeupNodeTypeOnSuccess = nodeType;
    }

    public final boolean isWakeupOneOnFailure() {
        return wakeupOneOnFailure;
    }

    public final void setWakeupOneOnFailure(boolean wakeupInd) {
        this.wakeupOneOnFailure = wakeupInd;
    }

    public final Object getWakeupNodeTypeOnFailure() {
        return wakeupNodeTypeOnFailure;
    }

    public final void setWakeupOneOnFailure(boolean wakeupInd, Object nodeType) {
        this.wakeupOneOnFailure = wakeupInd;
        this.wakeupNodeTypeOnFailure = nodeType;
    }

    //****************************************************************************************************************//
    //                                              5: SyncVisitConfig reset                                          //
    //****************************************************************************************************************//
    public final void reset() {
        this.nodeValue = null;
        this.nodeType = null;
        this.initState = null;
        this.parkSupport.reset();
        this.allowInterruption = true;

        this.wakeupOneOnFailure = true;
        this.wakeupOneOnSuccess = false;
        this.wakeupNodeTypeOnSuccess = null;
        this.wakeupNodeTypeOnFailure = null;
    }
}
