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
import org.stone.tools.CommonUtil;
import org.stone.tools.atomic.ReferenceFieldUpdaterImpl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;

/**
 * Phaser,a synchronization impl by wait pool
 *
 * @author Chris Liao
 * @version 1.0
 */
public class Phaser {
    //update to phase via cas when termination
    private static final GamePhase TerminatedPhase = new GamePhase(-1, 0, null);
    private static final AtomicReferenceFieldUpdater<Phaser, GamePhase> PhaseUpd = ReferenceFieldUpdaterImpl.newUpdater(Phaser.class, GamePhase.class, "phase");

    private final ResultWaitPool waitPool;
    private Phaser root;
    private Phaser parent;
    private volatile GamePhase phase;//current phase，it is a implementation of result call

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
        this.phase = new GamePhase(0, parties, this);//create a initial phase as default
        if (parent != null) parent.register();
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
        if (parties <= 0) throw new IllegalArgumentException("Parties must be greater than 0");

        for (; ; ) {
            GamePhase curPhase = this.phase;
            if (curPhase == TerminatedPhase) return TerminatedPhase.phaseNo;
            if (curPhase.isAllArrived()) continue;//all parties has been arrived of the phase,so continue
            if (curPhase.casTargetNumber(parties)) return curPhase.getPhaseNo();
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
        for (; ; ) {
            GamePhase curPhase = this.phase;
            if (curPhase == TerminatedPhase) return TerminatedPhase.getPhaseNo();
            if (curPhase.isAllArrived()) continue;//current Phase has over,so jump to read next

            if (curPhase.increaseArrivedCount()) {//increase an arrival success
                int phaseNo = curPhase.getPhaseNo();
                int targetNumber = curPhase.getTargetNumber();
                if (arrivalType == 2) targetNumber--;//arriveAndDeregister

                if (curPhase.isAllArrived()) {//all parties arrived,then wakeup all waiters in pool
                    int newPhaseNo = phaseNo + 1;
                    this.phase = new GamePhase(newPhaseNo, targetNumber, this);//create a new phase
                    this.waitPool.wakeupFirst(true, phaseNo, RUNNING);

                    //a new phase has generated
                    this.onAdvance(newPhaseNo, targetNumber);
                    return newPhaseNo;
                } else {//exists un-arrival
                    if (arrivalType == 2) {//arriveAndDeregister
                        curPhase.casTargetNumber(-1);
                        return phaseNo;
                    } else if (arrivalType == 3) {//arriveAndAwaitAdvance
                        return awaitAdvance();
                    }
                }
            }
        }//for end
    }

    //****************************************************************************************************************//
    //                                     5: Wait methods(3)                                                         //
    //****************************************************************************************************************//
    //wait for a new phase
    public int awaitAdvance() {
        SyncVisitConfig config = new SyncVisitConfig();
        config.setNodeType(phase.getPhaseNo());
        config.setPropagatedOnSuccess(true);
        config.allowInterruption(false);//forbidden interruption

        try {
            return doAwait(config);
        } catch (InterruptedException e) {
            return phase.getPhaseNo();
        }
    }

    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        SyncVisitConfig config = new SyncVisitConfig();
        config.setNodeType(phase);
        config.setPropagatedOnSuccess(true);

        return doAwait(config);
    }

    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        SyncVisitConfig config = new SyncVisitConfig(timeout, unit);
        config.setNodeType(phase);
        config.setPropagatedOnSuccess(true);

        int phaseNo = doAwait(config);
        if (config.getParkSupport().isTimeout()) throw new TimeoutException();
        return phaseNo;
    }

    private int doAwait(SyncVisitConfig config) throws InterruptedException {
        GamePhase currentPhase = this.phase;
        if (currentPhase == TerminatedPhase) return TerminatedPhase.phaseNo;

        //parameter PhaseNo equals the current PhaseNo,then waiting util new Phase
        if (CommonUtil.objectEquals(currentPhase.getPhaseNo(), config.getNodeType())) {
            try {
                waitPool.get(currentPhase, null, config);
                return this.phase.getPhaseNo();
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw new Error();
            }
        } else {
            return currentPhase.getPhaseNo();
        }
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
            this.waitPool.wakeupFirst(true, null, RUNNING);//wakeup all waiters in pool to end waiting
        }
    }

    //****************************************************************************************************************//
    //                                     7: Monitor methods(4)                                                      //
    //****************************************************************************************************************//
    public int getPhase() {
        return phase.getPhaseNo();
    }

    public int getRegisteredParties() {
        return phase.getTargetNumber();
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
        private final int phaseNo;
        private final Phaser owner;
        private final AtomicInteger targetNumber;
        private final AtomicInteger arrivedCount;

        GamePhase(int phaseNo, int targetNum, Phaser owner) {
            this.owner = owner;
            this.phaseNo = phaseNo;
            this.targetNumber = new AtomicInteger(targetNum);
            this.arrivedCount = new AtomicInteger(0);
        }

        int getPhaseNo() {
            return phaseNo;
        }

        int getTargetNumber() {
            return targetNumber.get();
        }

        int getArrivedCount() {
            return arrivedCount.get();
        }

        boolean isAllArrived() {
            return arrivedCount.get() >= targetNumber.get();
        }

        int getUnarrivedCount() {
            int count = targetNumber.get() - arrivedCount.get();
            return count > 0 ? count : 0;
        }

        boolean casTargetNumber(int incrCount) {
            int currentCount = targetNumber.get();
            return targetNumber.compareAndSet(currentCount, currentCount + incrCount);
        }

        boolean increaseArrivedCount() {
            if (targetNumber.get() == 0)
                throw new IllegalStateException("Attempted arrival of unregistered party for 0");
            int currentCount = arrivedCount.get();
            return arrivedCount.compareAndSet(currentCount, currentCount + 1);
        }

        public Object call(Object arg) {
            return owner.phase != this;
        }
    }
}
