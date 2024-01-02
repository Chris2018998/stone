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
package org.stone.tools.extend;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static java.lang.System.nanoTime;
import static java.lang.Thread.yield;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * Semaphore Implementation by ConcurrentLinkedQueue,its key logic is from my jdbc pool,you can visit the website:
 * https://github.com/Chris2018998/stone/beecp
 *
 * @author Chris Liao
 */
public class ConcurrentSemaphore {
    //initial state after being offered to wait queue
    private static final int STS_NORMAL = 0;
    //blocking state when not get a permit
    private static final int STS_WAITING = 1;
    //success state to get a permit
    private static final int STS_ACQUIRED = 2;
    //a chance to try getting a permit under unfair mode
    private static final int STS_TRY_ACQUIRE = 3;
    //failed state while timeout or interrupted on waiting
    private static final int STS_FAILED = 4;
    //minimum nano time in blocking,if less than it,then abandon blocking
    private static final long parkForTimeoutThreshold = 1000L;
    //max count for spin loop
    private static final int maxTimedSpins = Runtime.getRuntime().availableProcessors() < 2 ? 0 : 32;
    //a pre-prepared exception when interrupted from waiting
    private static final InterruptedException RequestInterruptException = new InterruptedException();
    //cas updater on an atomic integer
    private static final AtomicIntegerFieldUpdater<Borrower> updater = AtomicIntegerFieldUpdater
            .newUpdater(Borrower.class, "state");

    /**
     * self-defined synchronization object,detailed info about it,see{@link Sync} class.
     */
    private Sync sync;

    /**
     * constructor
     *
     * @param size is max number of permits
     * @param fair is a mode parameter on permit acquisition
     */
    public ConcurrentSemaphore(int size, boolean fair) {
        sync = fair ? new FairSync(size) : new NonfairSync(size);
    }

    /**
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit    the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     * if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquire(timeout, unit);
    }

    /**
     * Releases a permit, returning it to the semaphore.
     */
    public void release() {
        sync.release();
    }

    /**
     * Returns the current number of permits available in this semaphore.
     *
     * @return the number of permits available in this semaphore
     */

    public int availablePermits() {
        return sync.availablePermits();
    }

    /**
     * Queries whether any threads are waiting to acquire.
     *
     * @return {@code true} if there may be other threads waiting to acquire the lock
     */
    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public int getQueueLength() {
        return sync.getQueueLength();
    }

    //base Sync
    private static abstract class Sync {
        final ConcurrentLinkedQueue<Borrower> waitQueue;
        private final AtomicInteger permitCount;

        private Sync(int size) {
            this.permitCount = new AtomicInteger(size);
            this.waitQueue = new ConcurrentLinkedQueue<Borrower>();
        }

        public int availablePermits() {
            return permitCount.get();
        }

        //the returned value is a estimation number
        public int getQueueLength() {
            return waitQueue.size();
        }

        public boolean hasQueuedThreads() {
            return !waitQueue.isEmpty();
        }

        //try to wakeup a waiter by cas with a transferred int number
        private boolean transferToWaiter(int newCode, Borrower borrower) {
            int state;
            do {
                state = borrower.state;
                if (state != STS_NORMAL && state != STS_WAITING) return false;
            } while (!updater.compareAndSet(borrower, state, newCode));
            if (state == STS_WAITING) LockSupport.unpark(borrower.thread);
            return true;
        }

        protected boolean acquireOnePermit() {
            do {
                int expect = permitCount.get();
                if (expect == 0) return false;//no available permits
                if (permitCount.compareAndSet(expect, expect - 1)) return true;
            } while (true);
        }

        public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
            if (acquireOnePermit()) return true;

            boolean isFailed = false;
            boolean isInterrupted = false;
            final long deadline = nanoTime() + unit.toNanos(timeout);
            Borrower borrower = new Borrower();
            Thread thread = borrower.thread;
            waitQueue.offer(borrower);
            int spinSize = (waitQueue.peek() == borrower) ? maxTimedSpins : 0;

            do {
                int state = borrower.state;
                switch (state) {
                    case STS_ACQUIRED: {
                        return true;
                    }
                    case STS_TRY_ACQUIRE: {
                        if (acquirePermit()) {
                            waitQueue.remove(borrower);
                            return true;
                        }
                    }
                    case STS_FAILED: {
                        waitQueue.remove(borrower);
                        if (isInterrupted) throw RequestInterruptException;
                        return false;//timeout
                    }
                }

                if (isFailed) {//failed
                    if (borrower.state == state && updater.compareAndSet(borrower, state, STS_FAILED)) {
                        waitQueue.remove(borrower);
                        if (isInterrupted) throw RequestInterruptException;
                        return false;//timeout
                    }
                } else if (state == STS_TRY_ACQUIRE) {
                    borrower.state = STS_NORMAL;
                    yield();
                } else {//here:(state == STS_NORMAL)
                    timeout = deadline - nanoTime();
                    if (timeout - parkForTimeoutThreshold > parkForTimeoutThreshold) {
                        if (spinSize > 0) {
                            spinSize--;
                        } else if (updater.compareAndSet(borrower, STS_NORMAL, STS_WAITING)) {
                            parkNanos(timeout);
                            if (thread.isInterrupted()) {
                                isFailed = true;
                                isInterrupted = true;
                            }
                            //reset to normal
                            if (borrower.state == STS_WAITING)
                                updater.compareAndSet(borrower, STS_WAITING, isInterrupted ? STS_FAILED : STS_NORMAL);
                        }
                    } else if (timeout <= 0L) {//timeout
                        isFailed = true;
                    }
                }
            } while (true);
        }

        abstract void release();
    }

    private static final class FairSync extends Sync {
        public FairSync(int size) {
            super(size);
        }

        protected final boolean acquirePermit() {
            do {
                if (!waitQueue.isEmpty()) return false;

                int expect = usingSize.get();
                int update = expect + 1;
                if (update > size) return false;
                if (usingSize.compareAndSet(expect, update)) return true;
            } while (true);
        }

        public final void release() { //transfer permit
            Borrower borrower;
            while ((borrower = waitQueue.poll()) != null)
                if (transferToWaiter(STS_ACQUIRED, borrower)) return;
            usingSize.decrementAndGet();//release permit
        }
    }

    private static final class NonfairSync extends Sync {
        public NonfairSync(int size) {
            super(size);
        }

        public final void release() {//transfer permit
            usingSize.decrementAndGet();
            for (Borrower borrower : waitQueue)
                if (transferToWaiter(STS_TRY_ACQUIRE, borrower)) return;
        }
    }

    /**
     * permit borrower
     */
    private static final class Borrower {
        volatile int state = STS_NORMAL;
        Thread thread = Thread.currentThread();
    }
}