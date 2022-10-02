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
 * them on boarding,the wakeup count must equal the present seated size(exclude self),which can guarantee 'All-or-none'
 * rule,if not equal then cancel this flight(broken state,some wait threads timeout or interrupted),or the last passenger
 * still not be boarding during a time period,some passengers be loss of patience and abandon the flight,wakeup automatically
 * some passengers in sleeping to leave.
 * <p>
 * Cancelled flights not accept any new coming passengers(throws exception{@code BrokenBarrierException}),but can be reset
 * to new flights(call method{@link #reset}).
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CyclicBarrier2 extends ThreadWaitPool {
    //A new flight is ready to welcome passengers(door open)
    private static final int State_Open = 0;
    //Flight is on boarding（change to this value from 'State_Open' when first passenger reach）
    private static final int State_Board = 1;
    //Flight is in flying(execute action's method{@code run()}when the action property value is not null)
    private static final int State_Flying = 2;
    //Flight arrive destination(arrived destination,after trip action executing complete,state quickly changed to new and a new flight was set)
    private static final int State_Arrival = 3;
    //Flight canceled(some passengers left(timeout or interrupted) before setting out,flight can be reset a new)
    private static final int State_Cancelled = 4;

    //Current flight no(a property in chain node,count by this value when wakeup)
    private long flightNo;
    //current flight state(see state static definition)
    private AtomicInteger flightState;
    //Number of seats in flight room
    private int seatSize;
    //passenger count,which have been aboard,flight set out when full(passengerCount == seatSize)
    private AtomicInteger passengerCount;
    //Number of completed flying
    private int tripCount;
    //action execute at setting out
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

    //true,flight has been cancelled
    public boolean isBroken() {
        return flightState.get() == State_Cancelled;
    }

    //true,means all passengers be boarding(test validation method called in super class)
    public boolean testCondition() {
        return passengerCount.get() == seatSize;
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

    //await implement
    private int doAwait(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        nextTrip:
        for (; ; ) {
            if (isBroken()) throw new BrokenBarrierException();

            /*
             * 1: obtain boarding no of current flight with cas way,if the value is zero,then waiting
             * in the lobby of airport for next trip(wait in same chain,but wait type is 0).
             */
            int boardNo = tryGetBoardingNo();
            try {
                //2: the last passenger reach board,then wakeup other sleeping passengers
                if (boardNo == seatSize) {
                    //wakeup other passengers in same flight room,
                    int count = wakeupByType(flightNo);
                    if (count == seatSize - 1) {
                        flightState.set(State_Flying);//set out
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

                        //reset to a new flight
                        this.passengerCount.set(0);
                        this.flightNo = System.currentTimeMillis();
                        this.flightState.set(State_Open);
                        return boardNo;
                    }
                }

                //3:Waiting for wakeup
                try {
                    super.doWait(timeout, unit, boardNo > 0 ? flightNo : 0);
                    if (boardNo > 0) {
                        if (flightState.get() == State_Cancelled) throw new BrokenBarrierException();
                        return boardNo;
                    } else {//zero,waiting in lobby of airport
                        continue nextTrip;
                    }
                } catch (Throwable e) {
                    //mark flight state to cancelled(broken)
                    if (boardNo > 0 && flightState.get() == State_Board)
                        this.flightState.compareAndSet(State_Board, State_Cancelled);

                    if (e instanceof TimeoutException) throw (TimeoutException) e;
                    if (e instanceof InterruptedException) throw (InterruptedException) e;
                    BrokenBarrierException brokenBarrierException = new BrokenBarrierException();
                    brokenBarrierException.initCause(e);
                    throw brokenBarrierException;
                }
            } finally {
                if (boardNo > 0) wakeupByType(0);//wakeup passengers in lobby of airport
            }
        }
    }

    //try to get boarding number,successful,return positive number not greater then seat size value;failed,return 0.
    private int tryGetBoardingNo() {
        int c;
        do {
            c = this.passengerCount.get();
            if (c == seatSize) return 0;
            if (this.passengerCount.compareAndSet(c, c + 1)) {
                if (c == 0) flightState.set(State_Board);//is ready to passengers
                return c + 1;
            }
        } while (true);
    }

    //reset flight
    public boolean reset() {
        this.passengerCount.set(0);
        this.flightNo = System.currentTimeMillis();
        this.flightState.set(State_Open);
        wakeupAll();
        return true;
    }
}
