package org.stone.shine.util.concurrent.synchronizer.park;

import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.locks.LockSupport.parkUntil;

/**
 * Implement by {@code LockSupport.parkUntil}
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class MillisParkSupport implements ThreadParkSupport {
    private final long deadlineTime;
    private long parkNanos;
    private boolean hasTimeout;
    private boolean interrupted;

    public MillisParkSupport(long deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    public boolean block() {
        this.parkNanos = MILLISECONDS.toNanos(deadlineTime - System.currentTimeMillis());
        if (this.parkNanos > 0L) {
            parkUntil(this, deadlineTime);
            return this.interrupted = Thread.interrupted();
        } else {
            return this.hasTimeout = true;
        }
    }

    public boolean isTimeout() {
        return hasTimeout;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public long getLastParkNanos() {
        return parkNanos;
    }

    public String toString() {
        return "Implementation by method 'LockSupport.parkUntil(blocker,milliseconds)'";
    }
}
