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
 * The class instance can be seem as a theater and a set of(fixed sized)stage programs will perform
 * inside one by one(method{@link #countDown}called once means one program end),util all stage programs
 * completed(the atomic counter value reach zero),all watchers(wait threads)leave(exiting method{@link #await}),
 * then the theater closed and not reopen for ever.Luckly,you can create another new theater(or some)in your code.
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

    //count reach zero,then return true,this method will be called by super
    public boolean testCondition() {
        return count.get() == 0;
    }

    //***************************************************************************************************************//
    //                                      1:wait method(seat down to watch programs)                               //
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
    //                                          2:Programs method                                                     //
    //***************************************************************************************************************//
    public void countDown() {
        int c;
        do {
            c = this.count.get();
            if (c == 0) return;
            if (this.count.compareAndSet(c, c - 1)) {
                if (c == 1) wakeupAll();//the last program end,then notify all watchers to leave
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
