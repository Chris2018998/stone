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
 * We can image it's instance as a trip flight,when all passengers be on aboard,it then set out
 * this trip(execute a trip action,finished means trip was over and a new flight will begin,we call it cycle),
 * but one passenger(or some)be late,others would wait util him/them, maybe some aboard passengers be loss of patience
 * during waiting,then abandon this trip,then the flight marked as canceled(broken) and all aboard passengers
 * will leave,but it can reset to a new flight.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CyclicBarrier2 extends ThreadWaitPool {
    //A new flight is ready to welcome passengers(door open)
    private static final int State_Open = 0;
    //Flight is on boarding（change to this value from 'State_Open' when first passenger reach）
    private static final int State_Board = 1;
    //Flight is in flying(execute its {@code run()}when trip action is not null)
    private static final int State_Flying = 2;
    //Flight arrive destination(arrived destination,after trip action executing complete,state quickly changed to new and a new flight was set)
    private static final int State_Arrival = 3;
    //flight abandon(some passengers left(timeout or interrupted) before setting out,flight can be reset a new)
    private static final int State_Abandon = 4;

    //Current flight no(a property in chain node,count by this value when wakeup)
    private long flightNo;
    //current flight state(see state static definition)
    private AtomicInteger flightState;
    //Number of seats in flight room
    private int seatSize;
    //passenger count,which have been aboard,flight set out when full(passengerCount == seatSize)
    private AtomicInteger passengerCount;
    //store passenger boarding ordered number[1 -- seatSize]
    private ThreadLocal<Integer> passengerBoardOrder;
    //Number of completed flying
    private int tripCount;
    //action execute at setting out
    private Runnable tripAction;

    //***************************************************************************************************************//
    //                                         1:constructors                                                        //
    //***************************************************************************************************************//
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
        this.passengerBoardOrder = new ThreadLocal<>();
    }

    //***************************************************************************************************************//
    //                                          2:monitor methods                                                    //
    //***************************************************************************************************************//
    public int getTripCount() {
        return tripCount;
    }

    public boolean isBroken() {
        return flightState.get() == State_Abandon;
    }

    public boolean reset() {
        //@todo
        return true;
    }

    public void breakBarrier() {//break barrier
        //do nothing
    }

    //***************************************************************************************************************//
    //                                          3:aboard and wait for trip set out                                   //
    //***************************************************************************************************************//
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

    private int doAwait(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        if (isBroken()) throw new BrokenBarrierException();
        try {
            if (testCondition()) {
                synchronized (this) {
                    int orderNo = passengerBoardOrder.get();
                    if (orderNo == seatSize && flightState.get() == 1) {
                        flightState.set(2);//begin to trip
                        //super.wakeupWaiting();//wakeup all
                        tripCount++;
                        if (tripAction != null) try {
                            tripAction.run();
                        } catch (Throwable e) {
                            //trip error
                        }

                        flightState.set(0);//current trip over
                        return orderNo;
                    } else {
                        //@todo
                    }
                }
            }

            try {
                super.doWait(timeout, unit);
                if (flightState.get() == 3) throw new BrokenBarrierException();
                return passengerBoardOrder.get();
            } catch (BrokenBarrierException e) {
                throw e;
            } catch (InterruptedException e) {
                if (flightState.get() != 3) flightState.set(3);//broken
                //super.wakeupWaiting();//wakeup all
                throw e;
            } catch (TimeoutException e) {
                if (flightState.get() != 3) flightState.set(3);//broken
                //super.wakeupWaiting();//wakeup all
                throw e;
            } catch (Throwable e) {
                if (flightState.get() != 3) flightState.set(3);//broken
                //super.wakeupWaiting();//wakeup all
                throw new BrokenBarrierException(e.getMessage());
            }
        } finally {
            passengerBoardOrder.remove();
        }
    }

    public boolean testCondition() {
        passengerBoardOrder.remove();
        int c;
        do {
            c = this.passengerCount.get();
            if (c == seatSize) return true;
            if (this.passengerCount.compareAndSet(c, c + 1)) {
                if (c == 0) flightState.set(1);//gather is begin
                passengerBoardOrder.set(c + 1);//order no
                return c == seatSize - 1;
            }
        } while (true);
    }
}
