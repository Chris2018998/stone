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
import org.stone.shine.util.concurrent.synchronizer.validator.ResultEqualsValidator;

import static java.lang.System.nanoTime;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * Result Wait Pool,Outside caller to call pool's get method to take an object(execute ResultCall),
 * if not expected,then wait in pool util being wake-up by other or timeout/interrupted, and leave from pool.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ResultWaitPool extends ObjectWaitPool {
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
    //                                          2: get (3)                                                            //
    //****************************************************************************************************************//
    public final boolean isFair() {
        return !this.unfair;
    }

    public final Object get(ResultCall call, Object arg, SyncVisitConfig config) throws Exception {
        return get(call, arg, validator, config.getVisitTester(), config.getNodeType(), config.getNodeValue(),
                config.getParkNanos(), config.isAllowInterruption(), config.isPropagatedOnSuccess());
    }

    public final Object get(ResultCall call, Object arg, ResultValidator validator, SyncVisitTester tester,
                            Object nodeType, Object nodeValue, long parkNanos, boolean allowInterruption,
                            boolean propagatedOnSuccess) throws Exception {

        //1:check call parameter
        if (Thread.interrupted()) throw new InterruptedException();
        if (call == null || validator == null || tester == null)
            throw new IllegalArgumentException("Illegal argument,please check(call,validator,visitTester)");

        //2:test before call,if passed,then execute call
        if (tester.allow(unfair, nodeType, this)) {
            Object result = call.call(arg);
            if (validator.isExpected(result))
                return result;
        }

        //3:offer to wait queue
        byte spins = 1, postSpins = 1;
        final boolean isTime = parkNanos > 0L;
        SyncNode<Object> node = new SyncNode<>(nodeType, nodeValue);
        boolean atFirst = appendAsWaitNode(node);
        final long deadlineNanos = isTime ? nanoTime() + parkNanos : 0L;

        //4: spin
        do {
            if (atFirst) {//4.1: execute result call
                try {
                    do {
                        Object result = call.call(arg);
                        if (validator.isExpected(result)) {
                            waitQueue.poll();
                            if (propagatedOnSuccess) wakeupFirst(node.getType());
                            return result;
                        }
                    } while (spins > 0 && --spins > 0);
                    spins = postSpins = (byte) (postSpins << 1);
                } catch (Throwable e) {
                    waitQueue.poll();
                    wakeupFirst();
                    throw e;
                }
            }

            //4.2: try to park
            if (node.isNullState()) {
                if (isTime) {
                    parkNanos = deadlineNanos - nanoTime();
                    if (parkNanos <= 0L) {
                        removeAndWakeupFirst(node);
                        return validator.resultOnTimeout();
                    }
                    parkNanos(this, parkNanos);
                } else {
                    park(this);
                }

                if (Thread.interrupted() && allowInterruption) {
                    removeAndWakeupFirst(node);
                    throw new InterruptedException();
                }
            }

            //4.3: check node pos(reach here: node.state==RUNNING OR after parking)
            if (!atFirst) atFirst = waitQueue.peek() == node;
        } while (true);
    }
}
