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
 * this trip(execute a trip action, finished means trip was over and a new flight will begin,we call it cycle),
 * but one passenger(or some)be late,others would wait util him/them, maybe some aboard passengers be loss of patience
 * during waiting,then abandon this trip,then the flight marked as canceled(broken) and all aboard passengers
 * will leave,but it can reset to a new flight.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CyclicBarrier2 extends ThreadWaitPool {
    //a new flight
    private static final int State_New = 0;
    //passengers can be aboard（set to this value when first passenger aboard）
    private static final int State_Aboard = 1;
    //flight set out(execute trip action when it is not null)
    private static final int State_Flying = 2;
    //flight over(arrived destination,after trip action executing complete,state quickly changed to new and a new flight was set)
    private static final int State_Arrived = 3;
    //flight abandon(some passengers left(timeout or interrupted) before setting out,flight can be reset a new)
    private static final int State_Abandon = 4;

    //seat count in flight room
    private int seatSize;
    //passenger count,which have been aboard,flight set out when full(passengerCount == seatSize)
    private AtomicInteger passengerCount;
    //current flight state(see state static definition)
    private AtomicInteger tripState;
    //count of flying times(when arrived,then begin a new flight)
    private int tripCount;
    //action execute at setting out
    private Runnable tripAction;
    //store passenger boarding ordered number(1 -- seat count)
    private ThreadLocal<Integer> arrivalOrder = new ThreadLocal();


    public CyclicBarrier2(int size) {
        this(size, null);
    }

    public CyclicBarrier2(int size, Runnable tripAction) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.seatSize = size;
        this.tripAction = tripAction;
        this.tripState = new AtomicInteger(State_New);
        this.passengerCount = new AtomicInteger(0);
    }

    //***************************************************************************************************************//
    //                                          1:monitor methods                                                    //
    //***************************************************************************************************************//
    public int getTripCount() {
        return tripCount;
    }

    public boolean isBroken() {
        return tripState.get() == State_Abandon;
    }

    public boolean reset() {
        //@todo
        return true;
    }

    public void breakBarrier() {//break barrier
        //do nothing
    }

    //***************************************************************************************************************//
    //                                          2:aboard and wait for trip set out                                   //
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
                    int orderNo = arrivalOrder.get();
                    if (orderNo == seatSize && tripState.get() == 1) {
                        tripState.set(2);//begin to trip
                        super.wakeupWaiting();//wakeup all
                        tripCount++;
                        if (tripAction != null) try {
                            tripAction.run();
                        } catch (Throwable e) {
                            //trip error
                        }

                        tripState.set(0);//current trip over
                        return orderNo;
                    } else {
                        //@todo
                    }
                }
            }

            try {
                super.doWait(timeout, unit);
                if (tripState.get() == 3) throw new BrokenBarrierException();
                return arrivalOrder.get();
            } catch (BrokenBarrierException e) {
                throw e;
            } catch (InterruptedException e) {
                if (tripState.get() != 3) tripState.set(3);//broken
                super.wakeupWaiting();//wakeup all
                throw e;
            } catch (TimeoutException e) {
                if (tripState.get() != 3) tripState.set(3);//broken
                super.wakeupWaiting();//wakeup all
                throw e;
            } catch (Throwable e) {
                if (tripState.get() != 3) tripState.set(3);//broken
                super.wakeupWaiting();//wakeup all
                throw new BrokenBarrierException(e.getMessage());
            }
        } finally {
            arrivalOrder.remove();
        }
    }

    public boolean testCondition() {
        arrivalOrder.remove();
        int c;
        do {
            c = this.passengerCount.get();
            if (c == seatSize) return true;
            if (this.passengerCount.compareAndSet(c, c + 1)) {
                if (c == 0) tripState.set(1);//gather is begin
                arrivalOrder.set(c + 1);//order no
                return c == seatSize - 1;
            }
        } while (true);
    }
}
