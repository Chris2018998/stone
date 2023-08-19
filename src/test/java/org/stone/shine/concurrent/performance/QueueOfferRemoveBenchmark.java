package org.stone.shine.concurrent.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.shine.util.concurrent.ConcurrentLinkedDeque;
import org.stone.shine.util.concurrent.ConcurrentLinkedQueue;

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
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

public class QueueOfferRemoveBenchmark {
    public static Queue<Object> queue;
    @Param({"JDK-ConcurrentLinkedDeque", "JDK-ConcurrentLinkedQueue"})
    //@Param({"Stone-ConcurrentLinkedQueue", "Stone-ConcurrentLinkedDeque"})
    public String queueName;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(QueueOfferRemoveBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void queueTest() {
        Object item = new Object();
        queue.offer(item);
        queue.remove(item);
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        switch (queueName) {
            case "JDK-ConcurrentLinkedDeque":
                queue = new java.util.concurrent.ConcurrentLinkedDeque<Object>();
                break;
            case "JDK-ConcurrentLinkedQueue":
                queue = new java.util.concurrent.ConcurrentLinkedQueue<Object>();
                break;
            case "Stone-ConcurrentLinkedQueue":
                queue = new ConcurrentLinkedQueue<Object>();
                break;
            case "Stone-ConcurrentLinkedDeque":
                queue = new ConcurrentLinkedDeque<Object>();
                break;
        }
    }

    @TearDown(Level.Trial)
    public void teardown() {
        queue.clear();
        queue = null;
        System.gc();
    }
}
