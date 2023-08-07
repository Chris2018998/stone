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
import org.stone.tools.CommonUtil;
import org.stone.tools.atomic.UnsafeAdaptor;
import org.stone.tools.atomic.UnsafeAdaptorHolder;

import java.util.concurrent.TimeUnit;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;
import static org.stone.shine.util.concurrent.synchronizer.extend.AcquireTypes.TYPE_EXCLUSIVE;
import static org.stone.shine.util.concurrent.synchronizer.extend.AcquireTypes.TYPE_SHARED;

/**
 * Stamped Lock Impl
 * <p>
 * StampedLock是stone并发包下的一个读写锁实现，它的内部有一个64位的Volatile型长整数，此数由高32位和低32位组成,
 * 分别表达不同含义，长整数的初始值为：2147483648L（可视为32高位皆为0，低32位为最小负整数）。
 * <p>
 * 1：高32位数值表达锁状态：是否持有，持有多少次。读锁持有时，高位值可大于等于1(读锁具有重入性和共享性），写锁持有时，
 * 高位值只能等于1；等于0时，表示锁已被释放或未被锁定，低位值与锁前一致（不变化）。高32位数的最高位值永久性为0，既重入
 * 的次数最高可达2147483647次（最大整数），也就是说数字戳，它永久性是一个long正数{@code stamp >= 0 }
 * <p>
 * 2：低32位数为一个可递增的整数，它指征锁的类型，偶数时为写锁，奇数时为读锁，累加值，可更换锁的类型。加1则锁更换为
 * 下一个异类锁（奇转偶，偶转奇），加2则可获得下一个同类锁(奇转奇，偶转偶)。低位数递进过程可永无尽头，其奇偶替换规律，
 * 犹如阴阳替换，所以此锁又称之为：阴阳锁。（阴表示冷的，下降的，对应写锁；阳表示热的，上升的，对应读锁）
 * <p>
 * 3：抢锁成功后，32高位值位为1，32低位递进为奇数或偶数，上锁失败返回：-1
 * 4：当高位值大于0，低位数为偶数时(当前为写锁），线程抢锁失败。
 * 5：当高位值大于0，低位值为奇数时(当前为读锁)，且抢锁类型为读锁时，则累加高位数即可。
 * 6：当高位值等于0，低位值为偶数时(前次使用为写锁)，如果抢锁是读锁，高位设1， 低位值加1。
 * 7：当高位值等于0，低位值是奇数时(前次使用为读锁)，抢占锁是读锁时，高位设1，低位累加2。
 * 8：当高位值等于0，低位值为偶数时(前次使用为写锁），如果上抢占写锁，高位设1，低位加2。
 * 9：当高位值等于0，低位值为奇数时(前次使用为读锁），如果抢占写锁，高位设1，低位加1。
 * 10：写锁转读锁，低位加1，高位不变
 * 11：读锁转写锁，高位为0时，设高位为1，低位加1
 * <p>
 * 合并（高位|低位）的值，返回给锁的申请者。
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {
    private final static UnsafeAdaptor U;
    private static final long stampOffset;

    //****************************************************************************************************************//
    //                                          1: static(5)                                                          //
    //****************************************************************************************************************//
    private static final int MOVE_SHIFT = 32;
    private static final long CLN_HIGH_MASK = 0xFFFFFFFFL;//4294967295L;
    private static final int WRITE_LOCK_FLAG = 0;
    private static final int READ_LOCK_FLAG = 1;

    static {
        try {
            U = UnsafeAdaptorHolder.U;
            stampOffset = CommonUtil.objectFieldOffset(StampedLock.class, "stamp");
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //call wait Pool
    private volatile long stamp = 2147483648L;
    private ResultCall stampedReadCall = new ReadLockCall(this);
    private ResultCall stampedWriteCall = new WriteLockCall(this);
    private ResultValidator resultValidator = new LongResultValidator();
    private ResultWaitPool callWaitPool = new ResultWaitPool(false, resultValidator);

    //****************************************************************************************************************//
    //                                          2: CAS(1)                                                             //
    //****************************************************************************************************************//
    private static boolean compareAndSetLockStamp(StampedLock lock, long exp, long upd) {
        return U.compareAndSwapLong(lock, stampOffset, exp, upd);
    }

    //****************************************************************************************************************//
    //                                          3: Stamp(6)                                                           //
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

    private static boolean isSameStamp(long stamp1, long stamp2) {
        return stamp1 == stamp2 || (int) stamp1 == (int) stamp2;
    }

    private static boolean isTypeStamp(long stamp, int type) {
        int h = (int) (stamp >>> MOVE_SHIFT);
        int l = (int) (stamp & CLN_HIGH_MASK);
        return (l & 1) == type;
    }

    private static long getReleaseStamp(long stamp) {
        int h = (int) (stamp >>> MOVE_SHIFT);
        if (h > 0) {
            int l = (int) (stamp & CLN_HIGH_MASK);
            return ((long) h << MOVE_SHIFT) | (l & CLN_HIGH_MASK);
        }
        return stamp;
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
            return -1;
        } else {//read lock(Reentrant)
            if (h + 1 <= 0) throw new Error("Maximum lock count exceeded");
            h++;
        }

        return ((long) h << MOVE_SHIFT) | (l & CLN_HIGH_MASK);
    }

    //****************************************************************************************************************//
    //                                          4: Read Lock                                                          //
    //****************************************************************************************************************//
    public boolean isReadLocked() {
        long currentStamp = this.stamp;
        return isTypeStamp(currentStamp, READ_LOCK_FLAG) && getLockedCount(currentStamp) > 0;
    }

    public long tryReadLock() {
        long newStamp = genNextStamp(this.stamp, false);
        return newStamp > 0 && compareAndSetLockStamp(this, stamp, newStamp) ? newStamp : -1L;
    }

    public long readLock() {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_SHARED);
            config.setWakeupNextOnSuccess(true);
            config.setWakeupNodeTypeOnSuccess(TYPE_SHARED);
            config.setSupportInterrupted(false);
            return (long) callWaitPool.doCall(stampedReadCall, 1, config);
        } catch (Exception e) {
            return -1L;
        }
    }

    public long readLockInterruptibly() throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_SHARED);
            config.setWakeupNextOnSuccess(true);
            config.setWakeupNodeTypeOnSuccess(TYPE_SHARED);
            return (long) callWaitPool.doCall(stampedReadCall, 1, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return -1L;
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig(time, unit);
            config.setNodeType(TYPE_SHARED);
            config.setWakeupNextOnSuccess(true);
            config.setWakeupNodeTypeOnSuccess(TYPE_SHARED);
            return (long) callWaitPool.doCall(stampedReadCall, 1, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return -1L;
        }
    }

    public void unlockRead(long stamp) {
        long currentStamp = this.stamp;
        int inStampLow = lowInt(stamp);
        int curStampLow = lowInt(currentStamp);
        int curStampHigh = highInt(currentStamp);

        if (inStampLow == curStampLow && (curStampLow & 1) == READ_LOCK_FLAG && curStampHigh > 0) {
            long newStamp = contact(curStampHigh - 1, curStampLow);
            if (compareAndSetLockStamp(this, currentStamp, newStamp)) {
                if (highInt(newStamp) == 0) callWaitPool.wakeupOne(true, null, RUNNING);
            }
        }
    }

    public long tryConvertToReadLock(long stamp) {
        long currentStamp = this.stamp;
        if (currentStamp != stamp) return -1;

        int h = highInt(currentStamp);
        if (h == 1) {//locked
            int l = lowInt(currentStamp);
            if ((l & 1) == WRITE_LOCK_FLAG) { //read lock
                long newStamp = contact(h, l + 1);
                if (compareAndSetLockStamp(this, currentStamp, newStamp)) {//new read lock
                    this.callWaitPool.wakeupOne(true, TYPE_SHARED, RUNNING);//wakeup other share type
                    return newStamp;
                } else {
                    return -1L;
                }
            } else {
                return currentStamp;
            }
        } else {
            return -1L;
        }
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

    //****************************************************************************************************************//
    //                                          5: Write Lock                                                         //
    //****************************************************************************************************************//
    public boolean isWriteLocked() {
        long currentStamp = this.stamp;
        return isTypeStamp(currentStamp, WRITE_LOCK_FLAG) && getLockedCount(currentStamp) > 0;
    }

    public long tryWriteLock() {
        long newStamp = genNextStamp(this.stamp, true);
        return newStamp > 0 && compareAndSetLockStamp(this, stamp, newStamp) ? newStamp : -1L;
    }

    public long writeLock() {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_EXCLUSIVE);
            config.setSupportInterrupted(false);
            return (long) callWaitPool.doCall(stampedWriteCall, 1, config);
        } catch (Exception e) {
            return -1L;
        }
    }

    public long writeLockInterruptibly() throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig();
            config.setNodeType(TYPE_EXCLUSIVE);
            return (long) callWaitPool.doCall(stampedWriteCall, 1, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return -1L;
        }
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            SyncVisitConfig config = new SyncVisitConfig(time, unit);
            config.setNodeType(TYPE_EXCLUSIVE);
            return (long) callWaitPool.doCall(stampedWriteCall, 1, config);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return -1L;
        }
    }


    public void unlockWrite(long stamp) {
        long currentStamp = this.stamp;
        int inStampLow = lowInt(stamp);
        int curStampLow = lowInt(currentStamp);
        int curStampHigh = highInt(currentStamp);

        if (inStampLow == curStampLow && (curStampLow & 1) == WRITE_LOCK_FLAG && curStampHigh > 0) {
            long newStamp = contact(curStampHigh - 1, curStampLow);
            compareAndSetLockStamp(this, currentStamp, newStamp);
        }
    }

    public long tryConvertToWriteLock(long stamp) {
        long currentStamp = this.stamp;
        if (currentStamp != stamp) return -1;

        int h = highInt(currentStamp);
        if (h == 1) {//locked
            int l = lowInt(currentStamp);
            if ((l & 1) == READ_LOCK_FLAG) { //read lock
                long newStamp = contact(h, l + 1);
                if (compareAndSetLockStamp(this, currentStamp, newStamp)) {
                    return newStamp;
                } else {
                    return -1L;
                }
            } else {
                return currentStamp;
            }
        } else {
            return -1L;
        }
    }

    //****************************************************************************************************************//
    //                                          7: Lock Result Call                                                   //
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
            return -1L;
        }

        public boolean isExpected(Object result) {
            return ((long) result != -1L);
        }
    }
}
