/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stone.study.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * RunnableFuture implementation
 *
 * @author Chris Liao
 */

public class MyCountDownLatch {
    //waiter status
    private static final int WAITING = 0;//LockSupport.park()
    private static final int WAKEUP = 1;//LockSupport.unpark()
    private static final int FAILED = 2;//interrupt or timeout
    private static final AtomicIntegerFieldUpdater<Waiter> updater = AtomicIntegerFieldUpdater
            .newUpdater(Waiter.class, "state");
    private AtomicInteger atomicCount;
    private ConcurrentLinkedQueue<Waiter> waitQueue;

    public MyCountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.atomicCount = new AtomicInteger(count);
        waitQueue = new ConcurrentLinkedQueue<Waiter>();
    }

    public long getCount() {
        return atomicCount.get();
    }

    public void countDown() {
        int c;
        do {
            c = atomicCount.get();
            if (c == 0) return;
            if (atomicCount.compareAndSet(c, c - 1)) {
                if (c == 1) break;
                else return;
            }
        } while (true);

        //wakeup
        Waiter waiter;
        while ((waiter = waitQueue.poll()) != null) {
            if (updater.compareAndSet(waiter, WAITING, WAKEUP))
                LockSupport.unpark(waiter.thread);
        }
    }

    public void await() throws InterruptedException {
        await(0, TimeUnit.MILLISECONDS);
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (atomicCount.get() > 0) {
            Waiter waiter = new Waiter();
            waitQueue.add(waiter);
            LockSupport.park(unit.toNanos(timeout));
            if (waiter.state == WAITING && updater.compareAndSet(waiter, WAITING, FAILED)) {
                waitQueue.remove(waiter);
                if (waiter.thread.isInterrupted())
                    throw new InterruptedException();
            }
        }

        return atomicCount.get() == 0;
    }

    private static final class Waiter {
        volatile int state;
        Thread thread = Thread.currentThread();
    }
}
