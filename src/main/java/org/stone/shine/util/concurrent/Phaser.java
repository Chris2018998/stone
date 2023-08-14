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
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;

/**
 * Phaser Impl By Wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Phaser {
    private static final GamePhase TerminatedPhase = new GamePhase(-1, -1, null);
    private static final AtomicReferenceFieldUpdater<Phaser, GamePhase> PhaseUpd = ReferenceFieldUpdaterImpl.newUpdater(Phaser.class, GamePhase.class, "phase");
    private final ResultWaitPool waitPool;
    private Phaser root;
    private Phaser parent;
    private volatile GamePhase phase;

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
        this.phase = new GamePhase(0, parties, this);
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
            GamePhase curPhase = this.phase;
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
        config.setNodeType(phase.getPhaseNo());
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
        return phase == TerminatedPhase;
    }

    public void forceTermination() {
        GamePhase currentPhase = phase;
        if (currentPhase != TerminatedPhase && PhaseUpd.compareAndSet(this, currentPhase, TerminatedPhase)) {
            this.waitPool.wakeupOne(true, null, RUNNING);
        }
    }

    //****************************************************************************************************************//
    //                                     7: Monitor methods(4)                                                      //
    //****************************************************************************************************************//
    public int getPhase() {
        return phase.getPhaseNo();
    }

    public int getRegisteredParties() {
        return phase.getTargetCount();
    }

    public int getArrivedParties() {
        return phase.getArrivedCount();
    }

    public int getUnarrivedParties() {
        return phase.getUnarrivedCount();
    }

    public String toString() {
        return super.toString() +
                "[phase = " + getPhase() +
                " parties = " + getRegisteredParties() +
                " arrived = " + getArrivedParties() + "]";
    }

    //****************************************************************************************************************//
    //                                     8: Result Call Impl                                                        //
    //****************************************************************************************************************//
    private static class GamePhase implements ResultCall {
        private int phaseNo;
        private Phaser owner;
        private AtomicInteger targetCount;
        private AtomicInteger arrivedCount;

        GamePhase(int phaseNo, int expectedCount, Phaser owner) {
            this.owner = owner;
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

        int getUnarrivedCount() {
            int count = targetCount.get() - arrivedCount.get();
            return count > 0 ? count : 0;
        }

        boolean compareAndSetTargetCount(int expect, int update) {
            return targetCount.compareAndSet(expect, update);
        }

        boolean compareAndSetArrivedCount(int expect, int update) {
            return arrivedCount.compareAndSet(expect, update);
        }

        public Object call(Object arg) throws Exception {
            return owner.phase != this;
        }
    }
}
