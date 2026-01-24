/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeMethodLog;

import java.util.UUID;

/**
 * Default implementation of {@link BeeMethodLog}
 *
 * @author Chris Liao
 */
public class MethodLog<K> implements BeeMethodLog<K> {
    //Method call is in executing
    static final int State_Running = 0;
    //Method call is successful
    static final int State_Successful = 1;
    //Method call is failed
    static final int State_Failed = 2;

    //pooled key
    private final K key;
    //Log type
    private final int type;
    //log id
    private final String id;
    //pool name
    private final String poolName;

    //Method name of pool or (Statement,PreparedStatement,CallableStatement)
    private final String method;
    //Array of method parameters
    private final Object[] parameters;

    //Start time of method call,time unit:milliseconds
    private final long startTime;
    //End time of method call,time unit:milliseconds
    private long endTime;
    //End time of method call,time unit:milliseconds
    private int status = State_Running;

    //Result object of method call
    private transient Object resultObject;
    //Fail exception when method call
    private Throwable failureCause;

    //Removed flag
    private boolean removed;
    //Slow flag
    private boolean slow;
    //Long-running flag
    private boolean longRunning;
    //handled flag
    private boolean handled;

    public MethodLog(String poolName, K key, int type,
                     String method, Object[] parameters, long startTime) {

        this.key = key;
        this.poolName = poolName;
        this.type = type;
        this.method = method;
        this.parameters = parameters;
        this.id = UUID.randomUUID().toString();
        this.startTime = startTime;
    }

    public String getId() {
        return id;
    }

    public K getKey() {
        return key;
    }

    public int getType() {
        return type;
    }

    public String getPoolName() {
        return this.poolName;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isRunning() {
        return this.status == State_Running;
    }

    public boolean isSuccessful() {
        return this.status == State_Successful;
    }

    public boolean isException() {
        return this.status == State_Failed;
    }

    public boolean isSlow() {
        return slow;
    }

    public boolean hasHandledByListener() {
        return handled;
    }

    public boolean isLongRunning() {
        return this.longRunning;
    }


    void setHandled(boolean isHandled) {
        this.handled = isHandled;
    }

    void setAsSlow(long curTime, long slowThreshold) {
        if (this.endTime != 0L) {
            this.slow = this.endTime - this.startTime - slowThreshold >= 0L;
        } else {
            this.slow = this.longRunning = curTime - this.startTime - slowThreshold >= 0L;
        }
    }

    public Object getResult() {
        return resultObject;
    }

    void setResult(Object result, long endTime) {
        this.endTime = endTime;
        if (result instanceof Throwable) {
            this.failureCause = (Throwable) result;
            this.status = State_Failed;
        } else {
            this.resultObject = result;
            this.status = State_Successful;
        }
    }

    public Throwable getFailureCause() {
        return failureCause;
    }

    public boolean isRemoved() {
        return removed;
    }

    void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean equals(Object v) {
        return (v instanceof MethodLog) && this.id.equals(((MethodLog<?>) v).id);
    }
}
