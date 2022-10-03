/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone;

import org.jmin.stone.synchronizer.impl.ThreadWaitPool;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The class instance can be seemed as a flight,when passengers are full then begin a happy fly trip to a place,
 * after arriving destination,the flight set automatically as a new flight for next trip,we call it cyclic.
 * The passengers sleep(call method{@link #await} to add wait thread chain)after boarding,the last passenger will wakeup
 * them on boarding,the wakeup count must equal the present seated size(exclude self),if not equal then cancel this
 * flight(broken state,some wait threads timeout or interrupted),or the last passenger still not be boarding during a
 * time period,some passengers be loss of patience and abandon the flight,wakeup automatically some passengers in
 * sleeping to leave.Cancelled flights not accept any new coming passengers(throws exception{@code BrokenBarrierException}),
 * but can be reset as new flights(call method{@link #reset}).
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class CyclicBarrier2 extends ThreadWaitPool {
    //A new flight is ready to welcome passengers(door open)
    private static final int State_Open = 1;
    //Flight is on boarding（change to this value from 'State_Open' when first passenger reach）
    private static final int State_Board = 2;
    //Flight is in flying(execute action's method{@code run()}when the action property value is not null)
    private static final int State_Flying = 3;
    //Flight arrive destination(arrived destination,after trip action executing complete,state quickly changed to new)
    private static final int State_Arrival = 4;
    //Flight canceled(some passengers left(timeout or interrupted) before setting out,flight can be reset a new)
    private static final int State_Cancelled = 5;

    //Current flight no(a property in chain node,count by this value when wakeup)
    private long flightNo;
    //Current flight state(see state static definition)
    private AtomicInteger flightState;
    //Number of seats in flight room
    private int seatSize;
    //passenger count in room,which have been aboard,flight set out when full(passengerCount == seatSize)
    private AtomicInteger passengerCount;
    //Number of completed flying
    private int tripCount;
    //Execute when set out
    private Runnable tripAction;

    //****************************************************************************************************************//
    //                                         1:constructors                                                         //
    //****************************************************************************************************************//
    public CyclicBarrier2(int size) {
        this(size, null);
    }

    public CyclicBarrier2(int size, Runnable tripAction) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.seatSize = size;
        this.tripAction = tripAction;
        this.flightNo = System.currentTimeMillis();
        this.flightState = new AtomicInteger(State_Open);
        this.passengerCount = new AtomicInteger(0);
    }

    //****************************************************************************************************************//
    //                                          2: monitor methods                                                    //
    //****************************************************************************************************************//
    //number of completed flying(cyclic count)
    public int getTripCount() {
        return tripCount;
    }

    //return seat size in room(trip on full seated)
    public int getParties() {
        return seatSize;
    }

    //return waiting passengers in flight room
    public int getNumberWaiting() {
        int count = passengerCount.get();
        return count < seatSize ? count : seatSize - 1;
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
            return doAwait(0L, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            throw new Error(e);
        }
    }

    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        return doAwait(timeout, unit);
    }

    //await implement,return board ticket no(seat no)
    private int doAwait(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        if (Thread.interrupted()) throw new InterruptedException();

        nextTrip:
        for (; ; ) {
            if (isBroken()) throw new BrokenBarrierException();

            int seatNo = getBoardTicket();
            if (seatNo == seatSize) {//the last passenger coming
                //wakeup other boarding passengers(in sleeping)
                int awakeCount = wakeupByType(flightNo);
                if (awakeCount == seatSize - 1) {//others are wakeup
                    if (!flightState.compareAndSet(State_Board, State_Flying))//failed change to flying state
                        throw new BrokenBarrierException();

                    tripCount++;
                    if (tripAction != null) {
                        try {
                            tripAction.run();
                        } catch (Throwable e) {
                            //do nothing
                        }
                    }
                    //assume flight arrival
                    flightState.set(State_Arrival);

                    //set flight to new state(next trip begin)
                    this.passengerCount.set(0);
                    this.flightNo = System.currentTimeMillis();
                    this.flightState.set(State_Open);
                    this.wakeupByType(0);//wakeup passengers in lobby of airport to get ticket of next trip
                    return seatNo;
                } else {
                    if (flightState.get() == State_Board && flightState.compareAndSet(State_Board, State_Cancelled))
                        this.wakeupByType(0);//tell all passengers in lobby,the flight has cancel
                    throw new BrokenBarrierException();
                }
            }

            //3:Waiting for wakeup
            try {
                super.doWait(unit.toNanos(timeout), seatNo > 0 ? flightNo : 0);
                if (seatNo > 0) {
                    if (flightState.get() == State_Cancelled) throw new BrokenBarrierException();
                    return seatNo;
                } else {//zero,waiting in lobby of airport
                    continue nextTrip;
                }
            } catch (Throwable e) {
                //mark flight state to cancelled(broken)
                if (seatNo > 0) {//passenger of current flight
                    if (flightState.get() == State_Board && flightState.compareAndSet(State_Board, State_Cancelled))
                        this.wakeupByType(0);//tell all passengers in lobby,the flight has cancel
                }

                if (e instanceof TimeoutException) throw (TimeoutException) e;
                if (e instanceof InterruptedException) throw (InterruptedException) e;
                BrokenBarrierException brokenBarrierException = new BrokenBarrierException();
                brokenBarrierException.initCause(e);
                throw brokenBarrierException;
            }
        }
    }

    //take a boarding ticket,success,return a positive number(seat no),failed,return 0
    private int getBoardTicket() {
        int c;
        do {
            c = this.passengerCount.get();
            if (c == seatSize) return 0;
            if (this.passengerCount.compareAndSet(c, c + 1)) {
                if (c == 0)
                    flightState.set(State_Board);//sale out first tick of current flight,which set to open state here
                return c + 1;
            }
        } while (true);
    }

    //reset flight
    public boolean reset() {
        if (flightState.get() == State_Cancelled) {
            this.passengerCount.set(0);
            this.flightNo = System.currentTimeMillis();
            this.flightState.set(State_Open);
            wakeupAll();
            return true;
        } else {
            return false;
        }
    }
}
