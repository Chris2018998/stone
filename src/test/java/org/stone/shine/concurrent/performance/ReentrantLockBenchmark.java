package org.stone.shine.concurrent.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.shine.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@BenchmarkMode(Mode.Throughput)
//@Fork(1)
//@Measurement(iterations = 5, time = 1)
//@Warmup(iterations = 1, time = 1)

//@Fork(5)
//@Warmup(iterations=1,time = 1)
//@Measurement(iterations=5,time = 1)
//@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReentrantLockBenchmark {
    public static Lock lock;

    //@Param({"JDK-Lock", "Stone-Lock"})
    @Param({"Stone-Lock"})
    public String lockType;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ReentrantLockBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testLock() throws Exception {
        lock.lock();
        try {

        } finally {
            lock.unlock();
        }
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        switch (lockType) {
            case "JDK-Lock":
                lock = new java.util.concurrent.locks.ReentrantLock();
                break;
            case "Stone-Lock":
                lock = new ReentrantLock();
                break;
        }
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        System.gc();
    }
}
