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

import org.stone.shine.util.concurrent.synchronizer.SyncNodeStates;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * CyclicBarrier Impl By Wait Pool
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class CyclicBarrier {
    //A new flight is ready to welcome passengers(door open)
    public static final int State_Open = 1;
    //Flight is in flying(when passengers were full)
    public static final int State_Flying = 2;
    //Flight arrive destination(current trip was over)
    public static final int State_Arrived = 3;
    //Flight is in canceled(some passengers has exited the trip)
    public static final int State_Cancelled = 4;

    //Number of seats in flight room
    private final int seatSize;
    //Execute on setting out
    private final Runnable tripAction;
    //result call wait pool(driver core)
    private final ResultWaitPool waitPool;

    //Number of completed flying
    private int tripCount;
    //result call implementation(instance recreated after resetting flight to be a new one)
    private volatile GenerationFlight generationFlight;

    //****************************************************************************************************************//
    //                                         1:constructors                                                         //
    //****************************************************************************************************************//
    public CyclicBarrier(int size) {
        this(size, null);
    }

    public CyclicBarrier(int size, Runnable tripAction) {
        if (size <= 0) throw new IllegalArgumentException("size <= 0");
        this.seatSize = size;
        this.tripAction = tripAction;
        this.waitPool = new ResultWaitPool();
        this.generationFlight = new GenerationFlight(seatSize);
    }

    //****************************************************************************************************************//
    //                                          2: monitor methods                                                    //
    //****************************************************************************************************************//
    //return seat size in room(trip on full seated)
    public int getParties() {
        return seatSize;
    }

    //number of completed flying(cyclic count)
    public int getTripCount() {
        return tripCount;
    }

    //return passengers in flight room（wait util final passenger be on aboard)
    public int getNumberWaiting() {
        return generationFlight.getWaitingCount();
    }

    //return state of current flight(@see State_xxx static definition at first rows of this file body)
    public int getState() {
        return generationFlight.getState();
    }

    //true,flight has been cancelled(if exits passengers wait-timeout or Interrupted,all room passengers will leave)
    public boolean isBroken() {
        return generationFlight.isBroken();
    }

    //****************************************************************************************************************//
    //                                          3: board methods                                                      //
    //****************************************************************************************************************//
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            return doAwait(config);
        } catch (TimeoutException e) {
            throw new Error(e);
        }
    }

    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        SyncVisitConfig config = new SyncVisitConfig(timeout, unit);
        return doAwait(config);
    }

    //await implement,return board ticket no(seat no)
    private int doAwait(SyncVisitConfig config) throws InterruptedException, BrokenBarrierException, TimeoutException {
        if (Thread.interrupted()) throw new InterruptedException();

        while (true) {
            GenerationFlight currentFlight = generationFlight;
            if (currentFlight.isBroken()) throw new BrokenBarrierException();
            int seatNo = currentFlight.buyFlightTicket();//range[1 -- seatSize],0 means that not got a ticket
            long curFlightNo = seatNo > 0 ? currentFlight.flightNo : 0;
            config.setNodeType(curFlightNo);//flightNo using in wakeup

            try {
                //1: passenger gather in waiting pool(seatNo is zero,we can image that some passengers without ticket and waiting in hall for next flight)
                Object result = waitPool.get(currentFlight, seatNo, config);
                //2: call result is a false bool,exists one passenger wait timeout(the flight will be cancelled)
                if (Boolean.FALSE.equals(result)) throw new TimeoutException();

                //3: if result of the last call is boolean true,the flight room is seated full(set out)
                if (seatNo == seatSize && objectEquals(Boolean.TRUE, result)) {
                    //4: set the flight state to flying via cas
                    if (!currentFlight.compareAndSetState(State_Open, State_Flying)) throw new BrokenBarrierException();
                    //5: wakeup other room passengers(exit waiting)
                    waitPool.wakeupOne(true, curFlightNo, SyncNodeStates.RUNNING);

                    //6: if exists a trip action,execute it
                    tripCount++;
                    if (tripAction != null) {
                        try {
                            tripAction.run();
                        } catch (Throwable e) {
                            //don't throw out runtimeException or error from the trip action
                        }
                    }

                    //7: set flight state to be arrived(current flight is over)
                    currentFlight.setState(State_Arrived);

                    //8: create a new generation object(a new flight is ready)
                    this.generationFlight = new GenerationFlight(seatSize);

                    //9: wakeup hall passengers to buy ticket of the new flight
                    waitPool.wakeupAll(true, 0, SyncNodeStates.RUNNING);

                    //10: return the seat-No of the passenger)
                    return seatNo;
                }

                if (seatNo > 0) {
                    if (currentFlight.getState() == State_Cancelled) throw new BrokenBarrierException();
                    return seatNo;
                }
            } catch (Throwable e) {
                if (seatNo > 0) {
                    int state = currentFlight.getState();
                    if (state == State_Flying || state == State_Arrived) return seatNo;

                    //remark flight to be in cancelled state,notify all passengers to abandon
                    //current flight(include room and hall)
                    if (currentFlight.compareAndSetState(state, State_Cancelled))
                        waitPool.wakeupAll(true, null, SyncNodeStates.RUNNING);
                }

                if (e instanceof TimeoutException) throw (TimeoutException) e;
                if (e instanceof InterruptedException) throw (InterruptedException) e;
                if (e instanceof BrokenBarrierException) throw (BrokenBarrierException) e;
                if (seatNo > 0) {
                    BrokenBarrierException brokenException = new BrokenBarrierException();
                    brokenException.initCause(e);
                    throw brokenException;
                } else {
                    throw new Error(e);
                }
            }
        }//for expression end
    }

    //reset flight
    public synchronized boolean reset() {
        GenerationFlight currentFlight = generationFlight;
        int state = currentFlight.getState();
        if (state == State_Cancelled) {//
            this.generationFlight = new GenerationFlight(seatSize);
            return true;
        } else if (state == State_Open) {//reset boarding to new
            if (currentFlight.compareAndSetState(State_Open, State_Cancelled)) {
                this.generationFlight = new GenerationFlight(seatSize);
                waitPool.wakeupAll(true, null, SyncNodeStates.RUNNING);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    //****************************************************************************************************************//
    //                                          4: ResultCall Implement                                               //
    //****************************************************************************************************************//
    private static class GenerationFlight implements ResultCall {
        private final long flightNo;
        private final int seatSize;
        private final AtomicInteger flightState;
        private final AtomicInteger passengerCount;

        GenerationFlight(int seatSize) {
            this.seatSize = seatSize;
            this.flightNo = System.currentTimeMillis();
            this.passengerCount = new AtomicInteger(0);
            this.flightState = new AtomicInteger(State_Open);
        }

        //************************************************************************************************************//
        //                                           State methods                                                    //
        //************************************************************************************************************//
        int getState() {
            return flightState.get();
        }

        void setState(int state) {
            flightState.set(state);
        }

        boolean isBroken() {
            return flightState.get() == State_Cancelled;
        }

        boolean compareAndSetState(int expect, int update) {
            return flightState.compareAndSet(expect, update);
        }

        //************************************************************************************************************//
        //                                           passenger Count methods                                          //
        //************************************************************************************************************//
        int getWaitingCount() {
            if (flightState.get() == State_Open) {
                int count = passengerCount.get();
                return count < seatSize ? count : seatSize - 1;
            } else {
                return 0;
            }
        }

        //buy a boarding ticket,success,return a positive number(seat no),failed,return 0
        int buyFlightTicket() {
            int c;
            do {
                c = this.passengerCount.get();
                if (c == seatSize) return 0;
                if (this.passengerCount.compareAndSet(c, c + 1)) {
                    return c + 1;
                }
            } while (true);
        }

        //************************************************************************************************************//
        //                                           wait call method                                                 //
        //************************************************************************************************************//
        public Object call(Object arg) {
            if (flightState.get() > State_Flying) return true;

            return passengerCount.get() == seatSize;//full seated
        }
    }
}
