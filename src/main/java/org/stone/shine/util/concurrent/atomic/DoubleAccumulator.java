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
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.DoubleBinaryOperator;

/**
 * One or more variables that together maintain a running {@code double}
 * value updated using a supplied function.  When updates (method
 * {@link #accumulate}) are contended across threads, the set of variables
 * may grow dynamically to reduce contention.  Method {@link #get}
 * (or, equivalently, {@link #doubleValue}) returns the current value
 * across the variables maintaining updates.
 *
 * <p>This class is usually preferable to alternatives when multiple
 * threads update a common value that is used for purposes such as
 * summary statistics that are frequently updated but less frequently
 * read.
 *
 * <p>The supplied accumulator function should be side-effect-free,
 * since it may be re-applied when attempted updates fail due to
 * contention among threads. The function is applied with the current
 * value as its first argument, and the given update as the second
 * argument.  For example, to maintain a running maximum value, you
 * could supply {@code Double::max} along with {@code
 * Double.NEGATIVE_INFINITY} as the identity. The order of
 * accumulation within or across threads is not guaranteed. Thus, this
 * class may not be applicable if numerical stability is required,
 * especially when combining values of substantially different orders
 * of magnitude.
 *
 * <p>Class {@link DoubleAdder} provides analogs of the functionality
 * of this class for the common special case of maintaining sums.  The
 * call {@code new DoubleAdder()} is equivalent to {@code new
 * DoubleAccumulator((x, y) -> x + y, 0.0)}.
 *
 * <p>This class extends {@link Number}, but does <em>not</em> define
 * methods such as {@code equals}, {@code hashCode} and {@code
 * compareTo} because instances are expected to be mutated, and so are
 * not useful as collection keys.
 *
 * @author Doug Lea
 * @since 1.8
 */
public final class DoubleAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private final DoubleBinaryOperator function;
    private final long identity; // use long representation

    /**
     * Creates a new instance using the given accumulator function
     * and identity element.
     *
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @param identity            identity (initial value) for the accumulator function
     */
    public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity) {
        if (accumulatorFunction == null) throw new NullPointerException();
        this.function = accumulatorFunction;
        this.identity = Double.doubleToRawLongBits(identity);
        this.baseCell.value = this.identity;
    }

    /**
     * Updates with the given value.
     *
     * @param x the value
     */
    public void accumulate(double x) {
        if (!casCell(x, baseCell, function))
            doubleAccumulate(x, function);
    }

    /**
     * Returns the current value.  The returned value is <em>NOT</em>
     * an atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the value is being calculated might not be
     * incorporated.
     *
     * @return the current value
     */
    public double get() {
        Cell a;
        Cell[] as = cells;

        double result = Double.longBitsToDouble(baseCell.value);
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    result = function.applyAsDouble
                            (result, Double.longBitsToDouble(a.value));
            }
        }
        return result;
    }

    /**
     * Resets variables maintaining updates to the identity value.
     * This method may be a useful alternative to creating a new
     * updater, but is only effective if there are no concurrent
     * updates.  Because this method is intrinsically racy, it should
     * only be used when it is known that no threads are concurrently
     * updating.
     */
    public void reset() {
        Cell a;
        Cell[] as = cells;

        baseCell.value = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = identity;
            }
        }
    }

    /**
     * Equivalent in effect to {@link #get} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the value before reset
     */
    public double getThenReset() {
        Cell a;
        Cell[] as = cells;

        double result = Double.longBitsToDouble(baseCell.value);
        baseCell.value = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    double v = Double.longBitsToDouble(a.value);
                    a.value = identity;
                    result = function.applyAsDouble(result, v);
                }
            }
        }
        return result;
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return Double.toString(get());
    }

    /**
     * Equivalent to {@link #get}.
     *
     * @return the current value
     */
    public double doubleValue() {
        return get();
    }

    /**
     * Returns the {@linkplain #get current value} as a {@code long}
     * after a narrowing primitive conversion.
     */
    public long longValue() {
        return (long) get();
    }

    /**
     * Returns the {@linkplain #get current value} as an {@code int}
     * after a narrowing primitive conversion.
     */
    public int intValue() {
        return (int) get();
    }

    /**
     * Returns the {@linkplain #get current value} as a {@code float}
     * after a narrowing primitive conversion.
     */
    public float floatValue() {
        return (float) get();
    }

    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.DoubleAccumulator.SerializationProxy">
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
         * The current value returned by get().
         *
         * @serial
         */
        private final double value;
        /**
         * The function used for updates.
         *
         * @serial
         */
        private final DoubleBinaryOperator function;
        /**
         * The identity value
         *
         * @serial
         */
        private final long identity;

        SerializationProxy(DoubleAccumulator a) {
            function = a.function;
            identity = a.identity;
            value = a.get();
        }

        /**
         * Returns a {@code DoubleAccumulator} object with initial state
         * held by this proxy.
         *
         * @return a {@code DoubleAccumulator} object with initial state
         * held by this proxy.
         */
        private Object readResolve() {
            double d = Double.longBitsToDouble(identity);
            DoubleAccumulator a = new DoubleAccumulator(function, d);
            a.baseCell.value = Double.doubleToRawLongBits(value);
            return a;
        }
    }

}
