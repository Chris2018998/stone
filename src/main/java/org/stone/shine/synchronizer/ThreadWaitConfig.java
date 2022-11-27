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

public class ThreadWaitConfig {
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
    //wait time unit
    private TimeUnit waitTimeUnit;
    //time block object(using in LockSupport.park)
    private Object waitBlocker;
    //wait util deadline(using in LockSupport.parkUtil)
    private Date waitDeadline;

    //***********************************************C: other config**************************************************//
    //indicator of throw interruptException when interrupted
    private boolean throwsIE = true;
    //transfer got signal to other when got transfer signal but interrupted
    private boolean transferSignalOnIE = true;
    //indicator of whether remove wait node on leaving from pool
    private boolean removeOnLeave = true;

    //****************************************************************************************************************//
    //                                              1: node methods(7)                                                //
    //****************************************************************************************************************//
    public Object getNodeType() {
        return nodeType;
    }

    public void setNodeType(Object nodeType) {
        this.nodeType = nodeType;
    }

    public Object getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(Object nodeValue) {
        this.nodeValue = nodeValue;
    }

    public ThreadNode getWaitNode() {
        return waitNode;
    }

    public void setWaitNode(ThreadNode waitNode) {
        this.waitNode = waitNode;
    }

    public void setNodeValue(Object nodeType, Object nodeValue) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
    }

    //****************************************************************************************************************//
    //                                              2: wait time config(8)                                            //
    //****************************************************************************************************************//
    public void setMaxWaitTime(long maxWaitTime, TimeUnit waitTimeUnit) {
        this.maxWaitTime = maxWaitTime;
    }

    public void setMaxWaitTime(long maxWaitTime, TimeUnit waitTimeUnit, Object waitBlocker) {
        this.maxWaitTime = maxWaitTime;
        this.waitTimeUnit = waitTimeUnit;
        this.waitBlocker = waitBlocker;
    }

    public void setWaitDeadline(Date waitDeadline, Object waitBlocker) {
        this.waitDeadline = waitDeadline;
        this.waitBlocker = waitBlocker;
    }

    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    public TimeUnit getWaitTimeUnit() {
        return waitTimeUnit;
    }

    public Object getWaitBlocker() {
        return waitBlocker;
    }

    public Date getWaitDeadline() {
        return waitDeadline;
    }

    public void setWaitDeadline(Date waitDeadline) {
        this.waitDeadline = waitDeadline;
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
