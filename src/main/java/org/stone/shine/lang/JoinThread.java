/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.shine.lang;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * There are three join methods in Jdk {@link Thread},callers may be blocked in these methods invocation and wait for death notification of a target thread,
 * like this,other states should be supported on wait-notification mode? So I made a new one and name it to state join(JDK join should be named as time join?)
 * and provide four new join methods,refer to {@link #join(State)},{@link  #join(State, long)},{@link #joinAny(State[])},
 * {@link #joinAny(State[], long)}in this class and blocking implementation is provided in these methods by checking this thread state in a loop
 * and performance is not good,it is better that using synchronized keyword like jdk join methods.
 *
 * @author Chris Liao
 */

//public class Thread {
public class JoinThread extends Thread {

    //Park nanos in join methods for next check
    private static final long JoinParkNanos = 5L;

    /**
     * Waits on this thread util its state become expected state or {@link State#TERMINATED} state.
     *
     * @param state is a thread state value wait for
     * @return this thread state at exiting from the method
     * @throws InterruptedException while interrupted on wait
     */
    public Thread.State join(Thread.State state) throws InterruptedException {
        return join(state, 0L);
    }

    /**
     * Waits on this thread util its state become expected state or {@link State#TERMINATED} state.
     *
     * @param state  is a thread state value wait for
     * @param millis the time to wait in milliseconds
     * @return this thread state at exiting from the method
     * @throws InterruptedException while interrupted on wait
     */
    public Thread.State join(Thread.State state, long millis) throws InterruptedException {
        if (state == null) throw new IllegalArgumentException("Expected thread state can't be null");
        long endNanosTime = millis > 0L ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis) : 0L;

        if (endNanosTime > 0L) {
            for (; ; ) {
                Thread.State curState = this.getState();
                //1: state has changed to target state,so exit
                if (curState == State.TERMINATED || curState == state) return curState;

                //2: elapsed time has reach max wait time,so exit
                if (System.nanoTime() >= endNanosTime) return this.getState();
                //3: park 5 Nanos for next check
                LockSupport.parkNanos(JoinParkNanos);
                //4: check waiter thread state,if interrupted then exit
                if (Thread.interrupted()) throw new InterruptedException();
            }
        } else {
            for (; ; ) {
                Thread.State curState = this.getState();
                //1: state has changed to target state,so exit
                if (curState == State.TERMINATED || curState == state) return curState;
                //2: park 5 Nanos for next check
                LockSupport.parkNanos(JoinParkNanos);
                //3: check waiter thread state,if interrupted then exit
                if (Thread.interrupted()) throw new InterruptedException();
            }
        }
    }

    /**
     * Waits on this thread util its state become any expected state in array or {@link State#TERMINATED} state.
     *
     * @param states is state array value wait for one
     * @return this thread state at exiting from method
     * @throws InterruptedException while interrupted on wait
     */
    public Thread.State joinAny(Thread.State[] states) throws InterruptedException {
        if (states == null) throw new IllegalArgumentException("Expected thread states can't be null");
        if (states.length == 0) throw new IllegalArgumentException("Expected thread states can't be empty");
        return joinAny(states, 0L);
    }

    /**
     * Waits on this thread util its state become any expected state in array or {@link State#TERMINATED} state.
     *
     * @param states is state array value wait for one
     * @param millis the time to wait in milliseconds
     * @return this thread state at exiting
     * @throws InterruptedException while interrupted on wait
     */
    public Thread.State joinAny(Thread.State[] states, long millis) throws InterruptedException {
        if (states == null) throw new IllegalArgumentException("Expected thread states can't be null");
        if (states.length == 0) throw new IllegalArgumentException("Expected thread states can't be empty");
        long endNanosTime = millis > 0L ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis) : 0L;

        if (endNanosTime > 0L) {
            for (; ; ) {
                Thread.State curState = this.getState();
                //1: if terminated,then return terminated state
                if (curState == State.TERMINATED) return curState;

                //2: if match one,then return it
                for (Thread.State state : states)
                    if (state == curState) return curState;

                //3: elapsed time has reach max wait time,so exit
                if (System.nanoTime() >= endNanosTime) return this.getState();
                //4: park 5 nanoseconds for next check
                LockSupport.parkNanos(JoinParkNanos);
                //5: check waiter thread state,if interrupted then exit
                if (Thread.interrupted()) throw new InterruptedException();
            }
        } else {
            for (; ; ) {
                Thread.State curState = this.getState();
                //1: if terminated,then return terminated state
                if (curState == State.TERMINATED) return curState;

                //2: if match one,then return it
                for (Thread.State state : states)
                    if (state == curState) return curState;

                //3: park 5 Nanos for next check
                LockSupport.parkNanos(JoinParkNanos);
                //4: check waiter thread state,if interrupted then exit
                if (Thread.interrupted()) throw new InterruptedException();
            }
        }
    }
}
