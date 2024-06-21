package org.stone.shine.lang;

import java.util.concurrent.locks.LockSupport;

/**
 * Join extension on thread
 *
 * @author Chris Liao
 * @version 1.0
 */
//public class Thread {
public class JoinThread extends Thread {

    /**
     * Block caller thread util thread state change to target state
     *
     * @param targetState is an expected state
     */
    public void join(java.lang.Thread.State targetState) {
        for (; ; ) {
            java.lang.Thread.State curState = this.getState();
            if (curState == State.NEW || curState == State.TERMINATED || targetState == curState) {
                return;
            } else {
                LockSupport.parkNanos(5L);
            }
        }
    }

    /**
     * Block caller thread util thread state change to any of target states
     *
     * @param targetStates is an array of target states
     */
    public void join(java.lang.Thread.State[] targetStates) {
        if (targetStates == null) throw new IllegalArgumentException("target states can't be null");
        if (targetStates.length == 0) throw new IllegalArgumentException("target states can't be empty");
        for (; ; ) {
            java.lang.Thread.State curState = this.getState();
            if (curState == State.NEW || curState == State.TERMINATED) {
                return;
            } else {
                for (State state : targetStates) {
                    if (curState == state) return;
                }

                LockSupport.parkNanos(5L);
            }
        }
    }
}
