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

    public ResultWaitPool(final boolean fair) {
        this(fair, ResultEqualsValidator.BOOL_EQU_VALIDATOR);
    }

    public ResultWaitPool(final boolean fair, final ResultValidator validator) {
        //super(new SyncNodeChain());
        this.unfair = !fair;
        this.validator = validator;
    }

    //****************************************************************************************************************//
    //                                          3: wakeup for result wait pool(3)                                     //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return !this.unfair;
    }

    private void removeAndWakeupFirst(final SyncNode node) {
        this.waitQueue.remove(node);
        this.wakeupFirst();
    }

    public final void wakeupFirst() {
        final SyncNode first = this.waitQueue.peek();
        if (first != null && SyncNodeUpdater.casState(first, null, SyncNodeStates.RUNNING)) {
            LockSupport.unpark(first.getThread());
        }
    }

    public final void wakeupFirst(final Object wakeupType) {
        final SyncNode first = this.waitQueue.peek();
        if (first != null && (wakeupType == null || wakeupType == first.getType() || wakeupType.equals(first.getType()))) {
            if (SyncNodeUpdater.casState(first, null, SyncNodeStates.RUNNING)) LockSupport.unpark(first.getThread());
        }
    }

    //****************************************************************************************************************//
    //                                          3: get (2)                                                            //
    //****************************************************************************************************************//
    public Object get(final ResultCall call, final Object arg, final SyncVisitConfig config) throws Exception {
        return this.get(call, arg, this.validator, config.getVisitTester(), config.getNodeType(), config.getNodeValue(),
                config.isTimed(), config.getParkNanos(), config.isAllowInterruption(), config.isPropagatedOnSuccess());
    }

    public Object get(final ResultCall call, final Object arg, final ResultValidator validator, final SyncVisitTester tester,
                      final Object nodeType, final Object nodeValue, final boolean isTimed, final long parkNanos, final boolean allowInterruption,
                      final boolean propagatedOnSuccess) throws Exception {

        //1:check call parameter
        if (call == null || validator == null || tester == null)
            throw new NullPointerException("Exists null argument,please check(call,validator,visitTester)");
        if (allowInterruption && Thread.interrupted()) throw new InterruptedException();

        //2:test before call,if passed,then execute call
        if (tester.allow(this.unfair, nodeType, this)) {
            final Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3:offer to wait queue
        final SyncNode<Object> node = new SyncNode<>(nodeType, nodeValue);
        boolean executeInd = this.appendAsWaitNode(node);
        final long deadlineNanos = isTimed ? System.nanoTime() + parkNanos : 0L;

        //4: spin
        do {
            if (executeInd) {//4.1: execute result call
                try {
                    final Object result = call.call(arg);
                    if (validator.isExpected(result)) {
                        this.waitQueue.remove(node);
                        if (propagatedOnSuccess) wakeupFirst(nodeType);
                        return result;
                    }
                } catch (Throwable e) {
                    this.waitQueue.remove(node);
                    this.wakeupFirst();
                    throw e;
                }
            }

            //4.2: try to blocking caller util wake up by other
            if (!(executeInd = node.receivedSignal())) {
                if (isTimed) {
                    final long time = deadlineNanos - System.nanoTime();
                    if (time <= 0L) {
                        this.removeAndWakeupFirst(node);
                        return validator.resultOnTimeout();
                    }
                    LockSupport.parkNanos(this, time);
                } else {
                    LockSupport.park(this);
                }

                if (Thread.interrupted() && allowInterruption) {
                    this.removeAndWakeupFirst(node);
                    throw new InterruptedException();
                }
                executeInd = node.receivedSignal();
            }
        } while (true);
    }
}
