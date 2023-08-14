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

import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;

/**
 * Phaser Impl By Wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Phaser {
    private static final GamePhase TerminatedPhase = new GamePhase(-1, -1);
    private final ResultWaitPool waitPool;
    private Phaser root;
    private Phaser parent;
    private AtomicReference<GamePhase> phaseRef;

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
        if (parties < 0) throw new IllegalArgumentException();
        this.parent = parent;
        this.waitPool = new ResultWaitPool();
        this.phaseRef = new AtomicReference<GamePhase>(new GamePhase(0, parties));
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
        return bulkRegister(1);
    }

    public int bulkRegister(int parties) {
        if (parties < 0) throw new IllegalArgumentException();

        for (; ; ) {
            GamePhase curPhase = this.phaseRef.get();
            if (curPhase == TerminatedPhase) return TerminatedPhase.phaseNo;

            AtomicInteger atomicTargetCount = curPhase.targetCount;
            int targetCount = atomicTargetCount.get();
            if (targetCount == curPhase.arrivedCount.get()) continue;

            if (atomicTargetCount.compareAndSet(targetCount, targetCount + parties))
                return curPhase.getPhaseNo();
        }
    }

    //****************************************************************************************************************//
    //                                     4: arrive Method(2) -- parties                                             //
    //****************************************************************************************************************//
    public int arrive() {
        return doArrived(1);
    }

    public int arriveAndDeregister() {
        return doArrived(2);
    }

    public int arriveAndAwaitAdvance() {
        return doArrived(3);
    }

    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    //1: arrive 2: arriveAndDeregister 3: arriveAndAwaitAdvance
    private int doArrived(int arrivalType) {
        //@todo
        return 0;
    }

    //****************************************************************************************************************//
    //                                     5: Wait methods(3)                                                         //
    //****************************************************************************************************************//
    public int awaitAdvance() {
        SyncVisitConfig config = new SyncVisitConfig();
        config.setWakeupOneOnSuccess(true);
        config.setNodeType(phaseRef.get().phaseNo);
        config.allowInterruption(false);
        try {
            return doAwait(config);
        } catch (InterruptedException e) {
            return TerminatedPhase.getPhaseNo();
        }
    }

    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        SyncVisitConfig config = new SyncVisitConfig();
        config.setNodeType(phase);
        config.setWakeupOneOnSuccess(true);
        return doAwait(config);
    }

    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        SyncVisitConfig config = new SyncVisitConfig(timeout, unit);
        config.setNodeType(phase);
        config.setWakeupOneOnSuccess(true);
        int phaseNo = doAwait(config);
        if (config.getParkSupport().isTimeout()) throw new TimeoutException();
        return phaseNo;
    }

    private int doAwait(SyncVisitConfig config) throws InterruptedException {
        return 0;
        //@todo
    }

    //****************************************************************************************************************//
    //                                     6: Terminated methods(2)                                                   //
    //****************************************************************************************************************//
    public boolean isTerminated() {
        return phaseRef.get() == TerminatedPhase;
    }

    public void forceTermination() {
        GamePhase phase = phaseRef.get();
        if (phase != TerminatedPhase && phaseRef.compareAndSet(phase, TerminatedPhase)) {
            this.waitPool.wakeupOne(true, null, RUNNING);
        }
    }

    //****************************************************************************************************************//
    //                                     7: Monitor methods(3)                                                      //
    //****************************************************************************************************************//
    public final int getPhase() {
        return phaseRef.get().phaseNo;
    }

    public int getRegisteredParties() {
        return phaseRef.get().targetCount.get();
    }

    public int getArrivedParties() {
        return phaseRef.get().arrivedCount.get();
    }

    public int getUnarrivedParties() {
        int unarrival = phaseRef.get().targetCount.get() - phaseRef.get().arrivedCount.get();
        return unarrival > 0 ? unarrival : 0;
    }

    public String toString() {
        return "";
    }

    //****************************************************************************************************************//
    //                                     8: Result Call Impl                                                        //
    //****************************************************************************************************************//
    private static class GamePhase implements ResultCall {
        private int phaseNo;
        private AtomicInteger targetCount;
        private AtomicInteger arrivedCount;

        GamePhase(int phaseNo, int expectedCount) {
            this.phaseNo = phaseNo;
            this.targetCount = new AtomicInteger(expectedCount);
            this.arrivedCount = new AtomicInteger(0);
        }

        int getPhaseNo() {
            return phaseNo;
        }

        int getTargetCount() {
            return targetCount.get();
        }

        int getArrivedCount() {
            return arrivedCount.get();
        }

        boolean compareAndSetTargetCount(int expect, int update) {
            return targetCount.compareAndSet(expect, update);
        }

        boolean compareAndSetArrivedCount(int expect, int update) {
            return arrivedCount.compareAndSet(expect, update);
        }

        public Object call(Object arg) throws Exception {
            Phaser phaser = (Phaser) arg;
            return phaser.phaseRef.get() != this;
        }
    }
}
