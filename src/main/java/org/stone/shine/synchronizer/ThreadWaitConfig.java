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
 * Thread wait control parameter for wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ThreadWaitConfig {
    //************************************************A: wait node config*********************************************//
    //node type
    private Object nodeType;
    //node value
    private Object nodeValue;
    //node value
    private ThreadNode waitNode;

    //***********************************************B: wait time config**********************************************//
    //wait time value
    private long maxWaitTime;
    //wait util deadline(using in LockSupport.parkUtil)
    private boolean isMilliseconds;
    //time block object(using in LockSupport.park,LockSupport.parkNanos)
    private Object waitBlocker;

    //***********************************************C: other config**************************************************//
    //indicator of throw interruptException when interrupted
    private boolean throwsIE = true;
    //transfer got signal to other when got transfer signal but interrupted
    private boolean transferSignalOnIE = true;
    //indicator of whether remove wait node on leaving from pool
    private boolean removeOnLeave = true;

    //****************************************************************************************************************//
    //                                              1: node methods(3)                                                //
    //****************************************************************************************************************//
    public void setNodeValue(Object nodeType, Object nodeValue) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
    }

    public ThreadNode getWaitNode() {
        if (waitNode != null) return waitNode;
        return this.waitNode = ThreadWaitPool.createDataNode(nodeType, nodeValue);
    }

    public void setWaitNode(ThreadNode waitNode) {
        this.waitNode = waitNode;
    }

    //****************************************************************************************************************//
    //                                              2: wait time config(5)                                            //
    //****************************************************************************************************************//
    public void setMaxWaitTime(long maxWaitTime, TimeUnit waitTimeUnit) {
        this.setMaxWaitTime(maxWaitTime, waitTimeUnit, null);
    }

    public void setMaxWaitTime(long maxWaitTime, TimeUnit waitTimeUnit, Object waitBlocker) {
        if (waitTimeUnit == null) throw new IllegalArgumentException("timeUnit can't be null");
        this.maxWaitTime = waitTimeUnit.toNanos(maxWaitTime);
        this.waitBlocker = waitBlocker;
    }

    public void setWaitDeadline(Date waitDeadline) {
        this.setWaitDeadline(waitDeadline, null);
    }

    public void setWaitDeadline(Date waitDeadline, Object waitBlocker) {
        if (waitDeadline == null) throw new IllegalArgumentException("deadline can't be null");
        this.maxWaitTime = waitDeadline.getTime();
        this.waitBlocker = waitBlocker;
        this.isMilliseconds = true;
    }

    public ThreadParkSupport createThreadParkSupport() {
        if (waitBlocker != null) {
            return ThreadParkSupport.create(maxWaitTime, isMilliseconds, waitBlocker);
        } else {
            return ThreadParkSupport.create(maxWaitTime, isMilliseconds);
        }
    }

    //****************************************************************************************************************//
    //                                              3: others(6)                                                      //
    //****************************************************************************************************************//

    public boolean isThrowsIE() {
        return throwsIE;
    }

    public void setThrowsIE(boolean throwsIE) {
        this.throwsIE = throwsIE;
    }

    public boolean isTransferSignalOnIE() {
        return transferSignalOnIE;
    }

    public void setTransferSignalOnIE(boolean transferSignalOnIE) {
        this.transferSignalOnIE = transferSignalOnIE;
    }

    public boolean isRemoveOnLeave() {
        return removeOnLeave;
    }

    public void setRemoveOnLeave(boolean removeOnLeave) {
        this.removeOnLeave = removeOnLeave;
    }

}
