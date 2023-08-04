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
    //node object
    private SyncNode<E> syncNode;

    //***********************************************spin configuration***********************************************//
    //Park tool implement with class{@code java.util.concurrent.locks.LockSupport}
    private ThreadParkSupport parkSupport;
    //InterruptedException thrown indicator
    private boolean supportInterrupted = true;

    //***********************************************wakeup configuration*********************************************//
    //similar to AQS SHARED mode on acquisition success
    private boolean wakeupNextOnSuccess;
    //store some node type
    private Object wakeupNodeTypeOnSuccess;
    //similar to AQS CANCELLED
    private boolean wakeupNextOnFailure = true;
    //store some node type
    private Object wakeupNodeTypeOnFailure;

    //****************************************************************************************************************//
    //                                              1:constructors(4)                                                 //
    //****************************************************************************************************************//
    public SyncVisitConfig() {
        this(0, null, null, false);
    }

    public SyncVisitConfig(long parkTime, TimeUnit timeUnit) {
        this(parkTime, timeUnit, null, false);
    }

    public SyncVisitConfig(long parkTime, TimeUnit timeUnit, boolean isUtilTime) {
        this(parkTime, timeUnit, null, isUtilTime);
    }

    public SyncVisitConfig(long parkTime, TimeUnit timeUnit, Object blockObject, boolean isUtilTime) {
        if (parkTime > 0) {
            if (timeUnit == null) throw new IllegalArgumentException("time unit can't be null");
            if (isUtilTime) {
                long blockTimeMillis = timeUnit.toMillis(parkTime);
                this.parkSupport = blockObject == null ? new ThreadParkSupport.UtilMillsParkSupport1(blockTimeMillis) :
                        new ThreadParkSupport.UtilMillsParkSupport2(blockTimeMillis, blockObject);
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

    //****************************************************************************************************************//
    //                                              2:set node info(5)                                                 //
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

    public final SyncNode getSyncNode() {
        if (syncNode != null) return syncNode;
        return this.syncNode = new SyncNode<>(initState, nodeType, nodeValue);
    }

    //****************************************************************************************************************//
    //                                              3: spin configuration(3)                                         //
    //****************************************************************************************************************//
    public ThreadParkSupport getParkSupport() {
        return parkSupport;
    }

    public boolean supportInterrupted() {
        return supportInterrupted;
    }

    public void setSupportInterrupted(boolean supportInterrupted) {
        this.supportInterrupted = supportInterrupted;
    }

    //****************************************************************************************************************//
    //                                              4: wakeup configuration(6)                                        //
    //****************************************************************************************************************//
    public boolean isWakeupNextOnSuccess() {
        return wakeupNextOnSuccess;
    }

    public void setWakeupNextOnSuccess(boolean wakeupNextOnSuccess) {
        this.wakeupNextOnSuccess = wakeupNextOnSuccess;
    }

    public Object getWakeupNodeTypeOnSuccess() {
        return wakeupNodeTypeOnSuccess;
    }

    public void setWakeupNodeTypeOnSuccess(Object wakeupNodeTypeOnSuccess) {
        this.wakeupNodeTypeOnSuccess = wakeupNodeTypeOnSuccess;
    }

    public boolean isWakeupNextOnFailure() {
        return wakeupNextOnFailure;
    }

    public void setWakeupNextOnFailure(boolean wakeupNextOnFailure) {
        this.wakeupNextOnFailure = wakeupNextOnFailure;
    }

    public Object getWakeupNodeTypeOnFailure() {
        return wakeupNodeTypeOnFailure;
    }

    public void setWakeupNodeTypeOnFailure(Object wakeupNodeTypeOnFailure) {
        this.wakeupNodeTypeOnFailure = wakeupNodeTypeOnFailure;
    }

    //****************************************************************************************************************//
    //                                              5: SyncVisitConfig reset                                          //
    //****************************************************************************************************************//
    public final void reset() {
        this.nodeValue = null;
        this.nodeType = null;
        this.initState = null;
        this.syncNode = null;
        this.parkSupport.reset();
        this.supportInterrupted = true;

        this.wakeupNextOnFailure = true;
        this.wakeupNextOnSuccess = false;
        this.wakeupNodeTypeOnSuccess = null;
        this.wakeupNodeTypeOnFailure = null;
    }
}
