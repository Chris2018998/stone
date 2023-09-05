package org.stone.shine.util.concurrent.synchronizer.park;

import org.stone.shine.util.concurrent.synchronizer.ThreadParkSupport;

import java.util.concurrent.locks.LockSupport;

/**
 * Implement by {@code LockSupport.park}
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class BaseParkSupport implements ThreadParkSupport {
    private boolean interrupted;

    public boolean block() {
        LockSupport.park(this);
        return interrupted = Thread.interrupted();
    }

    public boolean isTimeout() {
        return false;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public long getLastParkNanos() {
        return 0L;
    }

    public String toString() {
        return "Implementation by method 'LockSupport.park(blocker)'";
    }
}
