/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent;

import org.stone.shine.synchronizer.ThreadParkSupport;
import org.stone.shine.synchronizer.base.ResultCall;
import org.stone.shine.synchronizer.base.ResultWaitPool;

import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.shine.synchronizer.ThreadNodeState.SIGNAL;

/**
 * The class instance can be seemed as a flight,when passengers are full then begin a happy fly trip to a place(then
 * return to take others for next trip),after arriving destination,the flight set automatically as a new flight for next
 * trip,we call it cyclic.The passengers sleep(call method{@link #await} to add wait thread chain)after boarding,the last
 * passenger will wakeup them on boarding,the wakeup count must equal the present seated size(exclude self),if not equal
 * then cancel this flight(broken state,some wait threads timeout or interrupted),or the last passenger still not be
 * boarding during a time period,some passengers be loss of patience and exit this trip,wakeup automatically some
 * passengers in sleeping to leave.the Cancelled flight not accept any new coming passengers(throws
 * exception{@code BrokenBarrierException}),but can be reset (call method{@link #reset}).
 * <p>
 * Road lines of state change
 * <pre>
 * {@code
 *  line A: State_Open(1) ---> State_Boarding(2) ---> State_Flying(3) ---> State_Arrived(4) ---> State_Open(1)
 * }
 * </pre>
 *
 * <pre>
 * {@code
 *   line B: State_Open(1) ---> State_Boarding(2) ---> State_Cancelled(5) ---> State_Open(1)
 * }
 * </pre>
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class CyclicBarrier implements ResultCall {
    //A new flight is ready to welcome passengers(door open)
    private static final int State_Open = 1;
    //Flight is in boarding(when the first ticket was sold）
    private static final int State_Boarding = 2;
    //Flight is in flying(when passengers were full)
    private static final int State_Flying = 3;
    //Flight arrive destination(current trip was over)
    private static final int State_Arrived = 4;
    //Flight is in canceled(some passengers has exited the trip)
    private static final int State_Cancelled = 5;
    //Flight no(set to a property of chain node,count by this value when wakeup)
    private final long flightNo;
    //Number of seats in flight room
    private final int seatSize;
    //Execute when set out
    private final Runnable tripAction;
    //Current flight state(see state static definition)
    private AtomicInteger flightState;
    //Number of passengers have been boarding
    private AtomicInteger passengerCount;
    //Number of completed flying
    private int tripCount;

    //result call wait pool
    private ResultWaitPool waitPool;

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
        this.flightNo = System.currentTimeMillis();
        this.flightState = new AtomicInteger(State_Open);
        this.passengerCount = new AtomicInteger(0);
        this.waitPool = new ResultWaitPool();
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

    //return waiting passengers in flight room
    public int getNumberWaiting() {
        int count = passengerCount.get();
        return count < seatSize ? count : seatSize - 1;
    }

    //count reach zero,which means all programs over
    public Object call(Object arg) {
        return Objects.equals(flightNo, arg) && passengerCount.get() == seatSize;
    }

    //true,flight has been cancelled
    public boolean isBroken() {
        return flightState.get() == State_Cancelled;
    }

    //****************************************************************************************************************//
    //                                          3: board methods                                                      //
    //****************************************************************************************************************//
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return doAwait(ThreadParkSupport.create());
        } catch (TimeoutException e) {
            throw new Error(e);
        }
    }

    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        return doAwait(ThreadParkSupport.create(unit.toNanos(timeout), false));
    }


    //await implement,return board ticket no(seat no)
    private int doAwait(ThreadParkSupport parker) throws InterruptedException, BrokenBarrierException, TimeoutException {
        if (Thread.interrupted()) throw new InterruptedException();

        //hall passengers can continue here for next trip
        while (true) {
            if (isBroken()) throw new BrokenBarrierException();

            int seatNo = buyFlightTicket();//range[1 -- seatSize],0 means that not got a ticket
            if (seatNo == seatSize) {//the last passenger coming
                //wakeup other passengers in room(sleeping)
                waitPool.wakeupAll(SIGNAL, flightNo);

                if (!flightState.compareAndSet(State_Boarding, State_Flying))//flying state change failed,means flight cancelled
                    throw new BrokenBarrierException();

                tripCount++;
                if (tripAction != null) {
                    try {
                        tripAction.run();
                    } catch (Throwable e) {
                        //don't throw out runtimeException or error from the trip action
                    }
                }
                //assume flight arrival
                flightState.set(State_Arrived);

                //set flight to new state(next trip begin)
                this.passengerCount.set(0);
                this.flightState.set(State_Open);
                waitPool.wakeupAll(SIGNAL);//wakeup hall passengers to buy ticket of next trip
                return seatNo;
            }

            //3:Waiting for wakeup
            try {
                //parameter zero means that passengers is in waiting hall(no ticket)
                Object callParameter = seatNo > 0 ? flightNo : 0;
                if (!waitPool.doCall(this, callParameter, Boolean.TRUE, parker, true, callParameter))
                    throw new TimeoutException();

                if (seatNo > 0) {
                    if (flightState.get() == State_Cancelled) throw new BrokenBarrierException();
                    return seatNo;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                if (seatNo > 0) {
                    int state = flightState.get();
                    if (state == State_Flying || state == State_Arrived) return seatNo;
                    if (state == State_Boarding && flightState.compareAndSet(state, State_Cancelled)) {
                        waitPool.wakeupAll();//notify all that the flight has cancelled
                    }
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

    //buy a boarding ticket,success,return a positive number(seat no),failed,return 0
    private int buyFlightTicket() {
        int c;
        do {
            c = this.passengerCount.get();
            if (c == seatSize) return 0;
            if (this.passengerCount.compareAndSet(c, c + 1)) {
                if (c == 0)
                    flightState.set(State_Boarding);//sale out first tick of current flight,which set to open state here
                return c + 1;
            }
        } while (true);
    }

    //reset flight
    public boolean reset() {
        if (flightState.get() == State_Cancelled) {//reset cancelled to new
            this.passengerCount.set(0);
            this.flightState.set(State_Open);
            waitPool.wakeupAll();
            return true;
        } else if (flightState.get() == State_Boarding) {//reset boarding to new
            this.flightState.set(State_Cancelled);
            waitPool.wakeupAll();
            this.passengerCount.set(0);
            this.flightState.set(State_Open);
            return true;
        } else {
            return false;
        }
    }
}