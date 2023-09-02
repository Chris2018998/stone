/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.locks;

import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.base.ResultValidator;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;
import org.stone.tools.atomic.UnsafeAdaptor;
import org.stone.tools.atomic.UnsafeAdaptorHolder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.stone.shine.util.concurrent.synchronizer.resource.AcquireTypes.TYPE_EXCLUSIVE;
import static org.stone.shine.util.concurrent.synchronizer.resource.AcquireTypes.TYPE_SHARED;

/**
 * StampedLock is a stamped digit Lock implementation by wait pool,which is
 * different to JDK{@code java.util.concurrent.locks.StampedLock}.This
 * implementation rely on an atomic long value and consist of two parts:
 * high-32 bits and low-32 bits,they can be convert to two independent integers,
 * high part represent hold count of a lock;low part represent lock body.
 * Initialization value of the atomic long is 2147483648L(high-32 bits is 0,
 * which can reach maximum integer;low-32 bits is a minimum integer:0x7fffffff).
 * If high part value equals zero,the lock is in idle state and can be acquired
 * and return a positive long to outside caller.
 * <p>
 * {@code stamp long = high-32 | low-32 }
 *
 * <h3>Lock mode</h3>
 * This stamped lock support two sub-lock mode:read mode and write mode,when the
 * low part is a even integer(positive or negative) means in write-mode lock and odd
 * integer is in read-mode lock.
 * <ul>
 * <li> {@code even integer == write-mode}</li>
 * <li> {@code odd integer == read-mode}</li>
 * </ul>
 *
 * <h3>Write Mode acquisition</h3>
 * a: if high is greater than zero,acquireFailedStamp number as result will return to callers
 * b: if high equals zero and low part is an odd integer(a read-mode digit),then set (high=1;low=low+1)
 * c: if high equals zero and low part is an even integer(a write-mode digit),then set (high=1;low=low+2)
 *
 * <h3>Read Mode acquisition</h3>
 * a: if high is greater than zero and low part is an even integer(a write-mode digit),acquireFailedStamp will be return
 * b: if high is greater than zero and low part is an odd integer(a read-mode digit),then set(high=high+1;low=low)
 * d: if high equals zero and low part is an even integer(a write-mode digit),then set(high=1;low=low+1)
 * e: if high equals zero and low part is an odd integer(a read-mode digit),then set(high=1;low=low+2)
 *
 * <h3>Lock Conversion</h3>
 * When high integer equals 1,the lock state can be converted between Read-mode and Write-mode,
 * related methods are below
 * <ul>
 * <li> {@link #tryConvertToReadLock} </li>
 * <li> {@link #tryConvertToWriteLock} </li>
 * </ul>
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {
    private static final int MOVE_SHIFT = 32;
    private static final long CLN_HIGH_MASK = 0xFFFFFFFFL;//4294967295L;
    private static final int WRITE_LOCK_FLAG = 0;
    private static final int READ_LOCK_FLAG = 1;
    private static final long acquireFailedStamp = -1L;
    private static final UnsafeAdaptor U;
    private static final long stampOffset;

    static {
        try {
            U = UnsafeAdaptorHolder.UA;
            stampOffset = U.objectFieldOffset(StampedLock.class.getDeclaredField("stamp"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //for call wait Pool
    private final ResultCall stampedReadCall = new ReadLockCall(this);
    private final ResultCall stampedWriteCall = new WriteLockCall(this);
    private final ResultValidator resultValidator = new LongResultValidator();
    private final ResultWaitPool callWaitPool = new ResultWaitPool(false, resultValidator);
    private volatile long stamp = 2147483648L;

    //lock views
    private ReadLockView readLockView;
    private WriteLockView writeLockView;
    private ReadWriteLockView readWriteLockView;

    //****************************************************************************************************************//
    //                                          1: CAS(1)                                                             //
    //****************************************************************************************************************//
    private static boolean compareAndSetLockStamp(StampedLock lock, long exp, long upd) {
        return U.compareAndSwapLong(lock, stampOffset, exp, upd);
    }

    //****************************************************************************************************************//
    //                                          2: Stamp(7)                                                           //
    //****************************************************************************************************************//
    private static int lowInt(long v) {
        return (int) (v & CLN_HIGH_MASK);
    }

    private static int highInt(long v) {
        return (int) (v >>> MOVE_SHIFT);
    }

    private static long contact(int h, int l) {
        return ((long) h << MOVE_SHIFT) | (l & CLN_HIGH_MASK);
    }

    private static int getLockedCount(long stamp) {
        return (int) (stamp >>> MOVE_SHIFT);
    }

    private static boolean isTypeStamp(long stamp, int type) {
        //int h = (int) (stamp >>> MOVE_SHIFT);
        int l = (int) (stamp & CLN_HIGH_MASK);
        return (l & 1) == type;
    }

    //generate next stamp
    private static long genNextStamp(long stamp, boolean acquireWrite) {
        int h = (int) (stamp >>> MOVE_SHIFT);
        int l = (int) (stamp & CLN_HIGH_MASK);
        boolean isWriteNum = (l & 1) == WRITE_LOCK_FLAG;//low is an even number(write type)

        if (h == 0) {//unlock
            h = 1;
            l += (acquireWrite == isWriteNum) ? 2 : 1;
        } else if (acquireWrite || isWriteNum) {//write lock and write or read lock and write lock
            return acquireFailedStamp;
        } else {//read lock(Reentrant)
            if (h + 1 <= 0) throw new Error("Maximum lock count exceeded");
            h++;
        }

        return ((long) h << MOVE_SHIFT) | (l & CLN_HIGH_MASK);
    }

    //****************************************************************************************************************//
    //                                          3: Read Lock(6)                                                       //
    //****************************************************************************************************************//
    public boolean isReadLocked() {
        long currentStamp = this.stamp;
        return isTypeStamp(currentStamp, READ_LOCK_FLAG) && getLockedCount(currentStamp) > 0;
    }

    public long tryReadLock() {
        long currentStamp = this.stamp;
        long newStamp = genNextStamp(currentStamp, false);
        return newStamp > 0 && compareAndSetLockStamp(this, currentStamp, newStamp) ? newStamp : acquireFailedStamp;
    }

    public long readLock() {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_SHARED);
            config.setPropagatedOnSuccess(true);
            config.allowInterruption(false);
            return (long) callWaitPool.get(stampedReadCall, null, config);
        } catch (Exception e) {
            return acquireFailedStamp;
        }
    }

    public long readLockInterruptibly() throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_SHARED);
            config.setPropagatedOnSuccess(true);
            return (long) callWaitPool.get(stampedReadCall, null, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return acquireFailedStamp;
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig(time, unit);
            config.setNodeType(TYPE_SHARED);
            config.setPropagatedOnSuccess(true);
            return (long) callWaitPool.get(stampedReadCall, null, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return acquireFailedStamp;
        }
    }

    public boolean unlockRead(long stamp) {
        long currentStamp = this.stamp;
        int inStampLow = lowInt(stamp);
        int curStampLow = lowInt(currentStamp);
        int curStampHigh = highInt(currentStamp);

        if (inStampLow == curStampLow && (curStampLow & 1) == READ_LOCK_FLAG && curStampHigh > 0) {
            long newStamp = contact(curStampHigh - 1, curStampLow);
            if (compareAndSetLockStamp(this, currentStamp, newStamp)) {
                if (highInt(newStamp) == 0) callWaitPool.wakeupFirst();
                return true;
            }
        }
        return false;
    }

    //****************************************************************************************************************//
    //                                          4: Write Lock(6)                                                      //
    //****************************************************************************************************************//
    public boolean isWriteLocked() {
        long currentStamp = this.stamp;
        return isTypeStamp(currentStamp, WRITE_LOCK_FLAG) && getLockedCount(currentStamp) > 0;
    }

    public long tryWriteLock() {
        long currentStamp = this.stamp;
        long newStamp = genNextStamp(currentStamp, true);
        return newStamp > 0 && compareAndSetLockStamp(this, currentStamp, newStamp) ? newStamp : acquireFailedStamp;
    }

    public long writeLock() {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_EXCLUSIVE);
            config.allowInterruption(false);
            return (long) callWaitPool.get(stampedWriteCall, null, config);
        } catch (Exception e) {
            return acquireFailedStamp;
        }
    }

    public long writeLockInterruptibly() throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_EXCLUSIVE);
            return (long) callWaitPool.get(stampedWriteCall, null, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return acquireFailedStamp;
        }
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig(time, unit);
            config.setNodeType(TYPE_EXCLUSIVE);
            return (long) callWaitPool.get(stampedWriteCall, null, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return acquireFailedStamp;
        }
    }

    public boolean unlockWrite(long stamp) {
        long currentStamp = this.stamp;
        int inStampLow = lowInt(stamp);
        int curStampLow = lowInt(currentStamp);
        int curStampHigh = highInt(currentStamp);

        if (inStampLow == curStampLow && (curStampLow & 1) == WRITE_LOCK_FLAG && curStampHigh > 0) {
            long newStamp = contact(curStampHigh - 1, curStampLow);
            if (compareAndSetLockStamp(this, currentStamp, newStamp)) {
                callWaitPool.wakeupFirst();
                return true;
            }
        }
        return false;
    }

    //****************************************************************************************************************//
    //                                          5: Lock Convert or others(4)                                          //
    //****************************************************************************************************************//
    public boolean validate(long inStamp) {
        long currentStamp = this.stamp;
        return currentStamp == inStamp || lowInt(currentStamp) == lowInt(inStamp);
    }

    public void unlock(long stamp) {
        if (getLockedCount(stamp) > 0) {
            if (isTypeStamp(stamp, READ_LOCK_FLAG)) {
                unlockRead(stamp);
            } else {
                unlockWrite(stamp);
            }
        }
    }

    public long tryConvertToReadLock(long stamp) {
        long currentStamp = this.stamp;
        if (currentStamp != stamp) return acquireFailedStamp;

        int h = highInt(currentStamp);
        if (h == 1) {//locked
            int l = lowInt(currentStamp);
            if ((l & 1) == WRITE_LOCK_FLAG) { //read lock
                long newStamp = contact(h, l + 1);
                if (compareAndSetLockStamp(this, currentStamp, newStamp)) {//new read lock
                    this.callWaitPool.wakeupFirst(TYPE_SHARED);//wakeup other share node
                    return newStamp;
                } else {
                    return acquireFailedStamp;
                }
            } else {
                return currentStamp;
            }
        } else {
            return acquireFailedStamp;
        }
    }

    public long tryConvertToWriteLock(long stamp) {
        long currentStamp = this.stamp;
        if (currentStamp != stamp) return -1;

        int h = highInt(currentStamp);
        if (h == 1) {//locked
            int l = lowInt(currentStamp);
            if ((l & 1) == READ_LOCK_FLAG) { //write lock
                long newStamp = contact(h, l + 1);
                if (compareAndSetLockStamp(this, currentStamp, newStamp)) {
                    return newStamp;
                } else {
                    return acquireFailedStamp;
                }
            } else {
                return currentStamp;
            }
        } else {
            return acquireFailedStamp;
        }
    }

    //****************************************************************************************************************//
    //                                          6: Lock View Method(3)                                                //
    //****************************************************************************************************************//
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v :
                (readLockView = new ReadLockView()));
    }

    public Lock asWriteLock() {
        StampedLock.WriteLockView v;
        return ((v = writeLockView) != null ? v :
                (writeLockView = new StampedLock.WriteLockView()));
    }

    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v :
                (readWriteLockView = new StampedLock.ReadWriteLockView()));
    }

    //****************************************************************************************************************//
    //                                          7: Lock Result Call class(3)                                          //
    //****************************************************************************************************************//
    private static class ReadLockCall implements ResultCall {
        private StampedLock lock;

        ReadLockCall(StampedLock lock) {
            this.lock = lock;
        }

        public Object call(Object arg) {
            return lock.tryReadLock();
        }
    }

    private static class WriteLockCall implements ResultCall {
        private StampedLock lock;

        WriteLockCall(StampedLock lock) {
            this.lock = lock;
        }

        public Object call(Object arg) {
            return lock.tryWriteLock();
        }
    }

    private static class LongResultValidator implements ResultValidator {

        public Object resultOnTimeout() {
            return acquireFailedStamp;
        }

        public boolean isExpected(Object result) {
            return ((long) result != acquireFailedStamp);
        }
    }

    //****************************************************************************************************************//
    //                                          8: Lock view class(3)                                                 //
    //****************************************************************************************************************//
    private final class ReadWriteLockView implements ReadWriteLock {
        public Lock readLock() {
            return asReadLock();
        }

        public Lock writeLock() {
            return asWriteLock();
        }
    }

    private final class ReadLockView implements Lock {
        private long lockStamp;

        public void lock() {
            this.lockStamp = readLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            this.lockStamp = readLockInterruptibly();
        }

        public boolean tryLock() {
            this.lockStamp = tryReadLock();
            return lockStamp != acquireFailedStamp;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            this.lockStamp = tryReadLock(time, unit);
            return lockStamp != acquireFailedStamp;
        }

        public void unlock() {
            if (lockStamp != acquireFailedStamp && unlockRead(lockStamp))
                lockStamp = acquireFailedStamp;
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    private final class WriteLockView implements Lock {
        private long lockStamp;

        public void lock() {
            this.lockStamp = writeLock();
        }

        public void lockInterruptibly() throws InterruptedException {
            this.lockStamp = writeLockInterruptibly();
        }

        public boolean tryLock() {
            this.lockStamp = tryWriteLock();
            return lockStamp != acquireFailedStamp;
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            this.lockStamp = tryWriteLock(time, unit);
            return lockStamp != acquireFailedStamp;
        }

        public void unlock() {
            if (lockStamp != acquireFailedStamp && unlockRead(lockStamp))
                lockStamp = acquireFailedStamp;
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
