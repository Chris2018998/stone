/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent;

import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phaser Impl By Wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Phaser {
    private final ResultWaitPool waitPool;
    private Phaser root;
    private Phaser parent;
    private volatile Phase currentPhase;

    //****************************************************************************************************************//
    //                                      1: Constructors(3)                                                        //
    //****************************************************************************************************************//
    public Phaser(int parties) {
        this(null, parties);
    }

    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    public Phaser(Phaser parent, int parties) {
        this.parent = parent;
        this.currentPhase = new Phase(0, parties);
        this.waitPool = new ResultWaitPool();
    }

    //****************************************************************************************************************//
    //                                      2: related Parent                                                         //
    //****************************************************************************************************************//
    public Phaser getRoot() {
        return root;
    }

    public Phaser getParent() {
        return parent;
    }

    //****************************************************************************************************************//
    //                                      3: add parties(2)                                                         //
    //****************************************************************************************************************//
    public int register() {
        return 0;
        //@todo
    }

    public int bulkRegister(int parties) {
        return 0;
        //@todo
    }

    //****************************************************************************************************************//
    //                                     4: arrive Method(2) -- parties                                             //
    //****************************************************************************************************************//
    public int arrive() {
        return 0;
        //@todo
    }

    public int arriveAndDeregister() {
        return 0;
        //@todo
    }

    public int arriveAndAwaitAdvance() {
        return 0;
        //@todo
    }

    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    //****************************************************************************************************************//
    //                                     5:Wait methods(3)                                                          //
    //****************************************************************************************************************//
    public int awaitAdvance() {
        return 0;
        //@todo
    }

    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        return 0;
        //@todo
    }

    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        return 0;
        //@todo
    }

    //****************************************************************************************************************//
    //                                     6: Terminated methods(2)                                                   //
    //****************************************************************************************************************//
    public boolean isTerminated() {
        return false;
    }

    public void forceTermination() {

    }

    //****************************************************************************************************************//
    //                                     7: Monitor methods(3)                                                      //
    //****************************************************************************************************************//
    public final int getPhase() {
        return 0;
        //@todo
    }

    public int getRegisteredParties() {
        return 0;
        //@todo
    }

    public int getArrivedParties() {
        return 0;
        //@todo
    }

    public int getUnarrivedParties() {
        return 0;
        //@todo
    }

    public String toString() {
        return "";
    }

    //****************************************************************************************************************//
    //                                     8: Result Call Impl                                                        //
    //****************************************************************************************************************//
    private static class Phase implements ResultCall {
        private int phaseNo;
        //maybe using a long to represent(atomic)
        private AtomicInteger registeredCount;
        private AtomicInteger arrivedCount;
        private AtomicInteger waitingCount;

        Phase(int phaseNo, int initRegisterCount) {
            this.phaseNo = phaseNo;
            this.registeredCount = new AtomicInteger(initRegisterCount);
            this.arrivedCount = new AtomicInteger(0);
            this.waitingCount = new AtomicInteger(0);
        }

        int getPhaseNo() {
            return phaseNo;
        }

        int getRegisteredCount() {
            return registeredCount.get();
        }

        int getArrivedCount() {
            return arrivedCount.get();
        }

        int getWaitingCount() {
            return waitingCount.get();
        }

        //do some thing(don't block thread in implementation method)
        public Object call(Object arg) throws Exception {
            int registerCount = registeredCount.get();
            return registerCount > 0 && registerCount == arrivedCount.get();//condition:leave from pool
        }
    }
}
