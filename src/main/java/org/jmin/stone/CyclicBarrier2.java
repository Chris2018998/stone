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
 * but one passenger( or some)be late, others would wait util him/them, maybe some aboard passengers be loss of patience
 * during waiting,then abandon this trip,  then the flight marked as canceled(broken) and all aboard passengers
 * will leave this flight and closed, but it can reset to a new flight.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CyclicBarrier2 extends ThreadWaitPool {
    private int seatSize;
    private AtomicInteger passengerCount;
    private AtomicInteger tripState;//0-init,1-gather,2-trip,3-broken
    private int tripCount;//fished trip total count
    private Runnable tripAction;
    private ThreadLocal<Integer> arrivalOrder = new ThreadLocal();

    public CyclicBarrier2(int size) {
        this(size, null);
    }

    public CyclicBarrier2(int size, Runnable tripAction) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.seatSize = size;
        this.tripAction = tripAction;
        this.tripState = new AtomicInteger(0);
        this.passengerCount = new AtomicInteger(0);
    }

    public int getTripCount() {
        return tripCount;
    }

    public boolean isBroken() {
        return tripState.get() == 3;
    }

    public boolean reset() {
        //@todo
        return true;
    }

    public void breakBarrier() {//break barrier
        //do nothing
    }

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
