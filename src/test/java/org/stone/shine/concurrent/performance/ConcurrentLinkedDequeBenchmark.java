package org.stone.shine.concurrent.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

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

public class ConcurrentLinkedDequeBenchmark {
    private static final Integer age = new Integer(45);
    public static Queue<Integer> queue;
    @Param({"JDK-ConcurrentLinkedDeque", "Stone-ConcurrentLinkedDeque"})
    //@Param({"ConcurrentLinkedQueue2"})
    public String queueName;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ConcurrentLinkedQueueBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static Queue<Integer> offerTest() throws Exception {
        queue.offer(age);
        return queue;
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        switch (queueName) {
            case "JDK-ConcurrentLinkedDeque":
                queue = new java.util.concurrent.ConcurrentLinkedDeque<Integer>();
                break;
            case "Stone-ConcurrentLinkedDeque":
                queue = new org.stone.shine.concurrent.ConcurrentLinkedDeque<Integer>();
                break;
        }
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        queue.clear();
        queue = null;
        System.gc();
    }
}
