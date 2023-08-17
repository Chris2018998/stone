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
    //greater than zero,it's value of time(or milliseconds)
    private final long parkTime;
    //time unit,convert parkTime to nanoSeconds
    private final TimeUnit timeUnit;
    //blocker object using in LockSupport.park
    private final Object blockObject;
    //true,the value of parkTime must be milliseconds
    private final boolean isUtilTime;

    //Park tool implement with class{@code java.util.concurrent.locks.LockSupport}
    private ThreadParkSupport parkSupport;
    //similar to AQS SHARED mode
    private boolean propagatedOnSuccess;
    //indicator:true,throws InterruptedException when waiting interrupted
    private boolean allowInterruption = true;

    //node value
    private E nodeValue;
    //node type
    private Object nodeType;
    //node init state
    private Object initState;
    //chain node
    private SyncNode<E> node;

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

    private SyncVisitConfig(long parkTime, TimeUnit timeUnit, Object blockObject, boolean isUtilTime) {
        if (parkTime > 0L && !isUtilTime && timeUnit == null)
            throw new IllegalArgumentException("Time unit can't be null");

        this.parkTime = parkTime;
        this.timeUnit = timeUnit;
        this.blockObject = blockObject;
        this.isUtilTime = isUtilTime;
    }

    //****************************************************************************************************************//
    //                                              2:set node info(5)                                                //
    //****************************************************************************************************************//
    public final Object getNodeType() {
        return this.nodeType;
    }

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
        if (node == null) node = new SyncNode<>(initState, nodeType, nodeValue);
        return node;
    }

    //****************************************************************************************************************//
    //                                              3: spin configuration(3)                                          //
    //****************************************************************************************************************//
    public final ThreadParkSupport getParkSupport() {
        if (parkSupport == null) {
            if (parkTime > 0L) {
                if (isUtilTime) {
                    this.parkSupport = blockObject == null ? new ThreadParkSupport.UtilMillsParkSupport1(parkTime) :
                            new ThreadParkSupport.UtilMillsParkSupport2(parkTime, blockObject);
                } else {
                    long blockNanos = timeUnit.toNanos(parkTime);
                    this.parkSupport = blockObject == null ? new ThreadParkSupport.NanoSecondsParkSupport(blockNanos) :
                            new ThreadParkSupport.NanoSecondsParkSupport2(blockNanos, blockObject);
                }
            } else {
                this.parkSupport = blockObject == null ? new ThreadParkSupport() :
                        new ThreadParkSupport.ThreadParkSupport2(blockObject);
            }
        }
        return parkSupport;
    }

    public final boolean isAllowInterruption() {
        return allowInterruption;
    }

    public void allowInterruption(boolean allowIndicator) {
        this.allowInterruption = allowIndicator;
    }

    public boolean isPropagatedOnSuccess() {
        return propagatedOnSuccess;
    }

    public void setPropagatedOnSuccess(boolean propagatedOnSuccess) {
        this.propagatedOnSuccess = propagatedOnSuccess;
    }

    //****************************************************************************************************************//
    //                                              4: SyncVisitConfig reset                                          //
    //****************************************************************************************************************//
    public final void reset() {
        this.node = null;
        this.nodeValue = null;
        this.nodeType = null;
        this.initState = null;
        this.parkSupport = null;
        this.allowInterruption = true;
        this.propagatedOnSuccess = false;
    }
}
