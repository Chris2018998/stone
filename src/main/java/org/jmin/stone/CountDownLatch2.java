/*
 * Copyright(C) Chris20189989(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone;

import org.jmin.stone.synchronizer.impl.ThreadWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The class instance can be seemed as a theater,and a set of(fixed sized)stage programs perform in its inside,
 * some people(threads)come into the theater to watch these show(call method{@link #await}).A atomic count property
 * represent number of the programs,one item of them done then call method{@link #countDown}to reduce one value from
 * the atomic variable util its value reach zero(all programs completed),then notify automatically all present watchers
 * to leave from the theater(call method{@link #wakeupAll}),which closed for ever.Luckly,you can create another new
 * theater(or some)in your code.One word to all:Welcome to my theater,it is open to the world.
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CountDownLatch2 extends ThreadWaitPool {
    //Number of programs(Remaining)
    private AtomicInteger count;

    public CountDownLatch2(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.count = new AtomicInteger(count);
    }

    //count reach zero,which means all programs over
    private boolean testCondition() {
        return count.get() == 0;
    }

    //****************************************************************************************************************//
    //                                      1:wait methods(seat down to watch programs)                               //
    //****************************************************************************************************************//
    //wait without time
    public void await() throws InterruptedException {
        if (testCondition()) return;
        try {
            super.doWait(0);
        } catch (TimeoutException e) {
            //do nothing
        }
    }

    //true means all programs over
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) throw new IllegalArgumentException("Time unit can't be null");
        if (testCondition()) return true;

        try {
            super.doWait(unit.toNanos(timeout));
        } catch (TimeoutException e) {
            //do nothing,the last item may be over at timeout point
        }
        return testCondition();
    }

    //****************************************************************************************************************//
    //                                          2:count method called after one show over                             //
    //****************************************************************************************************************//
    public void countDown() {
        int c;
        do {
            c = this.count.get();
            if (c == 0) return;
            if (this.count.compareAndSet(c, c - 1)) {
                if (c == 1) wakeupAll();//the last item end,then notify all watchers to leave
                return;
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          3:monitor method and instance desc                                    //
    //****************************************************************************************************************//
    //monitor method,return remained number of programs
    public long getCount() {
        return count.get();
    }

    //Description of instance
    public String toString() {
        return super.toString() + "[Count = " + count.get() + "]";
    }
}
