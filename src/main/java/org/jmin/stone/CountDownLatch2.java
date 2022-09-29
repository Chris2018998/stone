/*
 * Copyright(C) Chris2018998
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
 * This class instance can be seem as a theatre,two roles:one actor and one watcher(some watchers),
 * the actor need complete a set of show(times from class constructor),one done and the count value
 * reduce one(call method{@code #countDown}),when the count value reach zero,the show is over,
 * all watchers leave the theatre(exit await).
 *
 * @author Chris Liao
 * @version 1.0
 */
public class CountDownLatch2 extends ThreadWaitPool {

    //Times of show(Remaining)
    private AtomicInteger count;

    public CountDownLatch2(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.count = new AtomicInteger(count);
    }

    //count reach zero,then return true,this method will be called by super
    public boolean testCondition() {
        return count.get() == 0;
    }


    //***************************************************************************************************************//
    //                                          1:wait method(seat down to watch show)                               //
    //***************************************************************************************************************//
    public void await() throws InterruptedException {
        super.doWait();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            super.doWait(timeout, unit);
            return testCondition();
        } catch (TimeoutException e) {
            return false;
        }
    }

    //****************************************************************************************************************//
    //                                          2:Show method(for actor)                                              //
    //***************************************************************************************************************//
    public void countDown() {//means a show end
        int c;
        do {
            c = this.count.get();
            if (c == 0) return;
            if (this.count.compareAndSet(c, c - 1)) {
                if (c == 1) wakeupWaiting();//reach zero,show is over,then wakeup all viewers leave
                return;
            }
        } while (true);
    }

    //monitor method,return watcher count
    public long getCount() {
        return count.get();
    }

    //Description,just return watcher count
    public String toString() {
        return super.toString() + "[Count = " + count.get() + "]";
    }
}
