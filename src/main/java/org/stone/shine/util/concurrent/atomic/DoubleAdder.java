/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.stone.shine.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.DoubleBinaryOperator;

/**
 * One or more variables that together maintain an initially zero
 * {@code double} sum.  When updates (method {@link #add}) are
 * contended across threads, the set of variables may grow dynamically
 * to reduce contention.  Method {@link #sum} (or, equivalently {@link
 * #doubleValue}) returns the current total combined across the
 * variables maintaining the sum. The order of accumulation within or
 * across threads is not guaranteed. Thus, this class may not be
 * applicable if numerical stability is required, especially when
 * combining values of substantially different orders of magnitude.
 *
 * <p>This class is usually preferable to alternatives when multiple
 * threads update a common value that is used for purposes such as
 * summary statistics that are frequently updated but less frequently
 * read.
 *
 * <p>This class extends {@link Number}, but does <em>not</em> define
 * methods such as {@code equals}, {@code hashCode} and {@code
 * compareTo} because instances are expected to be mutated, and so are
 * not useful as collection keys.
 *
 * @author Doug Lea
 * @since 1.8
 */
public final class DoubleAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    private static final DoubleBinaryOperator operator = new DoubleAddrOperator();

    //initial value
    private final long initial;

    /*
     * Note that we must use "long" for underlying representations,
     * because there is no compareAndSet for double, due to the fact
     * that the bitwise equals used in any CAS implementation is not
     * the same as double-precision equals.  However, we use CAS only
     * to detect and alleviate contention, for which bitwise equals
     * works best anyway. In principle, the long/double conversions
     * used here should be essentially free on most platforms since
     * they just re-interpret bits.
     */

    /**
     * Creates a new adder with initial sum of zero.
     */
    public DoubleAdder() {
        this.initial = 0L;
    }

    /**
     * Creates a new adder with initial value.
     */
    public DoubleAdder(double initial) {
        this.initial = Double.doubleToRawLongBits(initial);
        this.baseCell.value = this.initial;
    }

    /**
     * Adds the given value.
     *
     * @param x the value to add
     */
    public void add(double x) {
        if (!casCell(x, baseCell, operator)) doubleAccumulate(x, operator);
    }

    /**
     * Returns the current sum.  The returned value is <em>NOT</em> an
     * atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the sum is being calculated might not be
     * incorporated.  Also, because floating-point arithmetic is not
     * strictly associative, the returned result need not be identical
     * to the value that would be obtained in a sequential series of
     * updates to a single variable.
     *
     * @return the sum
     */
    public double sum() {
        Cell a;
        Cell[] as = cells;

        double sum = Double.longBitsToDouble(baseCell.value);
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += Double.longBitsToDouble(a.value);
            }
        }
        return sum;
    }

    /**
     * Resets variables maintaining the sum to zero.  This method may
     * be a useful alternative to creating a new adder, but is only
     * effective if there are no concurrent updates.  Because this
     * method is intrinsically racy, it should only be used when it is
     * known that no threads are concurrently updating.
     */
    public void reset() {
        Cell a;
        Cell[] as = cells;

        this.baseCell.value = this.initial;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0L;
            }
        }
    }

    /**
     * Equivalent in effect to {@link #sum} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the sum
     */
    public double sumThenReset() {
        Cell a;
        Cell[] as = cells;

        double sum = Double.longBitsToDouble(baseCell.value);
        this.baseCell.value = this.initial;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    long v = a.value;
                    a.value = 0L;
                    sum += Double.longBitsToDouble(v);
                }
            }
        }
        return sum;
    }

    /**
     * Returns the String representation of the {@link #sum}.
     *
     * @return the String representation of the {@link #sum}
     */
    public String toString() {
        return Double.toString(sum());
    }

    /**
     * Equivalent to {@link #sum}.
     *
     * @return the sum
     */
    public double doubleValue() {
        return sum();
    }

    /**
     * Returns the {@link #sum} as a {@code long} after a
     * narrowing primitive conversion.
     */
    public long longValue() {
        return (long) sum();
    }

    /**
     * Returns the {@link #sum} as an {@code int} after a
     * narrowing primitive conversion.
     */
    public int intValue() {
        return (int) sum();
    }

    /**
     * Returns the {@link #sum} as a {@code float}
     * after a narrowing primitive conversion.
     */
    public float floatValue() {
        return (float) sum();
    }

    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.DoubleAdder.SerializationProxy">
     * SerializationProxy</a>
     * representing the state of this instance.
     *
     * @return a {@link SerializationProxy}
     * representing the state of this instance
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * @param s the stream
     * @throws java.io.InvalidObjectException always
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

    /**
     * Serialization proxy, used to avoid reference to the non-public
     * Striped64 superclass in serialized forms.
     *
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * The current value returned by sum().
         *
         * @serial
         */
        private final double value;

        SerializationProxy(DoubleAdder a) {
            value = a.sum();
        }

        /**
         * Returns a {@code DoubleAdder} object with initial state
         * held by this proxy.
         *
         * @return a {@code DoubleAdder} object with initial state
         * held by this proxy.
         */
        private Object readResolve() {
            DoubleAdder a = new DoubleAdder();
            a.baseCell.value = Double.doubleToRawLongBits(value);
            return a;
        }
    }

    private static class DoubleAddrOperator implements DoubleBinaryOperator {
        public final double applyAsDouble(double left, double right) {
            return left + right;
        }
    }
}
