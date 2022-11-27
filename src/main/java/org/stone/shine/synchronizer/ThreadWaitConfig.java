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

/**
 * Thread wait control parameter in wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ThreadWaitConfig {
    //node type
    private Object nodeType;
    //node value
    private Object nodeValue;
    //node value
    private ThreadNode waitNode;

    //indicator of throw interruptException when interrupted
    private boolean throwsIE = true;
    //transfer got signal to other when wakeup by
    private boolean wakeupOtherOnIE = true;
    //indicator of whether remove wait node on leaving from pool
    private boolean removeOnLeave = true;

    //****************************************************************************************************************//
    //                                              1: constructors(3)                                                //
    //****************************************************************************************************************//
    public ThreadWaitConfig(ThreadNode waitNode) {
        this.waitNode = waitNode;
    }

    public ThreadWaitConfig(Object nodeType) {
        this.nodeType = nodeType;
    }

    public ThreadWaitConfig(Object nodeType, Object nodeValue) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
    }

    //****************************************************************************************************************//
    //                                              2: get/set                                                        //
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

    public boolean isThrowsIE() {
        return throwsIE;
    }

    public void setThrowsIE(boolean throwsIE) {
        this.throwsIE = throwsIE;
    }

    public boolean isWakeupOtherOnIE() {
        return wakeupOtherOnIE;
    }

    public void setWakeupOtherOnIE(boolean wakeupOtherOnIE) {
        this.wakeupOtherOnIE = wakeupOtherOnIE;
    }

    public boolean isRemoveOnLeave() {
        return removeOnLeave;
    }

    public void setRemoveOnLeave(boolean removeOnLeave) {
        this.removeOnLeave = removeOnLeave;
    }
}
