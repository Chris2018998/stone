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

import org.stone.shine.util.concurrent.synchronizer.chain.SyncNode;
import org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeChain;
import org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeStates;
import org.stone.shine.util.concurrent.synchronizer.chain.SyncNodeUpdater;
import org.stone.shine.util.concurrent.synchronizer.validator.ResultEqualsValidator;

import java.util.concurrent.locks.LockSupport;

/**
 * Result Wait Pool,Outside caller to call pool's get method to take an object(execute ResultCall),
 * if not expected,then wait in pool util being wake-up by other or timeout/interrupted, and leave from pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ResultWaitPool extends ObjectWaitPool {
    //true,use unfair mode
    private final boolean unfair;
    //result validator(bool validator is default)
    private final ResultValidator validator;

    //****************************************************************************************************************//
    //                                          1: constructors(3)                                                    //
    //****************************************************************************************************************//
    public ResultWaitPool() {
        this(false, ResultEqualsValidator.BOOL_EQU_VALIDATOR);
    }

    public ResultWaitPool(boolean fair) {
        this(fair, ResultEqualsValidator.BOOL_EQU_VALIDATOR);
    }

    public ResultWaitPool(boolean fair, ResultValidator validator) {
        super(new SyncNodeChain());
        this.unfair = !fair;
        this.validator = validator;
    }

    //****************************************************************************************************************//
    //                                          3: wakeup for result wait pool(3)                                     //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return !this.unfair;
    }

    private void removeAndWakeupFirst(SyncNode node) {
        waitQueue.remove(node);
        wakeupFirst();
    }

    public final void wakeupFirst() {
        SyncNode first = waitQueue.peek();
        if (first != null && SyncNodeUpdater.casState(first, null, SyncNodeStates.RUNNING))
            LockSupport.unpark(first.getThread());
    }

    public final void wakeupFirst(Object wakeupType) {
        SyncNode first = waitQueue.peek();
        if (first != null && (wakeupType == null || wakeupType == first.getType() || wakeupType.equals(first.getType())))
            if (SyncNodeUpdater.casState(first, null, SyncNodeStates.RUNNING)) LockSupport.unpark(first.getThread());
    }

    //****************************************************************************************************************//
    //                                          3: get (2)                                                            //
    //****************************************************************************************************************//
    public Object get(ResultCall call, Object arg, SyncVisitConfig config) throws Exception {
        return get(call, arg, validator, config.getVisitTester(), config.getNodeType(), config.getNodeValue(),
                config.getParkNanos(), config.isAllowInterruption(), config.isPropagatedOnSuccess());
    }

    public Object get(ResultCall call, Object arg, ResultValidator validator, SyncVisitTester tester,
                      Object nodeType, Object nodeValue, long parkNanos, boolean allowInterruption,
                      boolean propagatedOnSuccess) throws Exception {

        //1:check call parameter
        if (call == null || validator == null || tester == null)
            throw new NullPointerException("Exists null argument,please check(call,validator,visitTester)");
        if (allowInterruption && Thread.interrupted()) throw new InterruptedException();

        //2:test before call,if passed,then execute call
        if (tester.allow(unfair, nodeType, this)) {
            Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3:offer to wait queue
        int spins = 1, postSpins = 1;
        final boolean isTime = parkNanos > 0L;
        final SyncNode<Object> node = new SyncNode<>(nodeType, nodeValue);
        boolean executeInd = appendAsWaitNode(node);
        final long deadlineNanos = isTime ? System.nanoTime() + parkNanos : 0L;

        //4: spin
        do {
            if (executeInd) {//4.1: execute result call
                try {
                    do {
                        Object result = call.call(arg);
                        if (validator.isExpected(result)) {
                            waitQueue.poll();
                            if (propagatedOnSuccess) wakeupFirst(nodeType);
                            return result;
                        }

                        if (spins <= 0) break;
                        if (--spins == 0) {
                            spins = postSpins = (byte) (postSpins << 1 | 1);
                            break;
                        }
                    } while (true);

                    executeInd=false;
                } catch (Throwable e) {
                    waitQueue.poll();
                    wakeupFirst();
                    throw e;
                }
            }

            //4.2: try to park
            if (node.getState()==SyncNodeStates.RUNNING) {
                node.setState(null);
                executeInd = true;
            } else {
                if (isTime) {
                    parkNanos = deadlineNanos - System.nanoTime();
                    if (parkNanos <= 0L) {
                        removeAndWakeupFirst(node);
                        return validator.resultOnTimeout();
                    }
                    LockSupport.parkNanos(this, parkNanos);
                } else {
                    LockSupport.park(this);
                }
                if (Thread.interrupted() && allowInterruption) {
                    removeAndWakeupFirst(node);
                    throw new InterruptedException();
                }

                if (node.getState()==SyncNodeStates.RUNNING) {
                    node.setState(null);
                    executeInd = true;
                }
            }
        } while (true);
    }
}
