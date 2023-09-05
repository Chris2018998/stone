package org.stone.shine.util.concurrent.synchronizer.park;

import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;

import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * Implement by {@code LockSupport.parkNanos}
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class NanosParkSupport implements ThreadParkSupport {
    private final long deadlineTime;
    private long parkNanos;
    private boolean hasTimeout;
    private boolean interrupted;

    public NanosParkSupport(long parkNanos) {
        this.deadlineTime = System.nanoTime() + parkNanos;
    }

    public boolean park() {
        if ((this.parkNanos = deadlineTime - System.nanoTime()) > 0L) {
            parkNanos(this, parkNanos);
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
        return "Implementation by  method 'LockSupport.parkNanos(blocker,time)'";
    }
}