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

import java.util.Date;
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
    //chain node
    private SyncNode<E> node;

    //parkTime(@see LockSupport.parkNanos)
    private long parkNanos;
    //parkTime(@see LockSupport.parkUtil)
    private Date deadlineDate;
    //Park tool implement with class{@code java.util.concurrent.locks.LockSupport}
    private ThreadParkSupport parkSupport;

    //similar to AQS SHARED mode
    private boolean propagatedOnSuccess;
    //indicator:true,throws InterruptedException when waiting interrupted
    private boolean allowInterruption = true;
    //visit tester
    private SyncVisitTester visitTester = SyncVisitTester.BASE_VISIT_TESTER;

    //****************************************************************************************************************//
    //                                              1:constructors(3)                                                 //
    //****************************************************************************************************************//
    public SyncVisitConfig() {
    }

    public SyncVisitConfig(Date deadlineDate) {
        if (deadlineDate == null) throw new IllegalArgumentException("Deadline date can't be null");
        this.deadlineDate = deadlineDate;
    }

    public SyncVisitConfig(long time, TimeUnit timeUnit) {
        if (timeUnit == null) throw new IllegalArgumentException("Time unit can't be null");
        if (time <= 0L) throw new IllegalArgumentException("Time must be greater than zero");
        this.parkNanos = timeUnit.toNanos(time);
    }

    //****************************************************************************************************************//
    //                                              2:set node info(4)                                                //
    //****************************************************************************************************************//
    public final Object getNodeType() {
        return this.nodeType;
    }

    public final void setNodeType(Object nodeType) {
        this.nodeType = nodeType;
    }

    public final void setNodeInitInfo(Object type, E value) {
        this.nodeType = type;
        this.nodeValue = value;
    }

    public final SyncNode<E> getSyncNode() {
        if (node != null) return node;
        return this.node = new SyncNode<>(nodeType, nodeValue);
    }

    //****************************************************************************************************************//
    //                                              3: spin configuration(3)                                          //
    //****************************************************************************************************************//
    public final boolean isAllowInterruption() {
        return allowInterruption;
    }

    public void allowInterruption(boolean allowIndicator) {
        this.allowInterruption = allowIndicator;
    }

    public final SyncVisitTester getVisitTester() {
        return visitTester;
    }

    public void setVisitTester(SyncVisitTester visitTester) {
        if (visitTester != null) this.visitTester = visitTester;
    }

    public boolean isPropagatedOnSuccess() {
        return propagatedOnSuccess;
    }

    public void setPropagatedOnSuccess(boolean propagatedOnSuccess) {
        this.propagatedOnSuccess = propagatedOnSuccess;
    }

    public final ThreadParkSupport getParkSupport() {
        if (parkSupport != null) return parkSupport;
        if (deadlineDate != null)
            return this.parkSupport = new ThreadParkSupport.DateUtilParkSupport(deadlineDate.getTime());
        if (parkNanos == 0) return this.parkSupport = new ThreadParkSupport();
        return this.parkSupport = new ThreadParkSupport.NanosParkSupport(parkNanos);
    }
}
