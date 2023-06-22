package org.stone.shine.concurrent.atomic;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.stone.shine.util.concurrent.atomic.LongAdder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@State(value = Scope.Benchmark)
public class BenchMarkTest {

    private static final LongAdder LONG_ADDER_VALUE = new LongAdder();
    private static final LongAdder LONG_ADDER_VALUE2 = new LongAdder();
    private static final AtomicLong ATOMIC_LONG_VALUE = new AtomicLong(0);

    @Param(value = {"100"})
    private int thread;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchMarkTest.class.getSimpleName())
                .warmupIterations(3)// 预热3轮
                .measurementTime(TimeValue.seconds(1))
                .measurementIterations(5)// 度量5轮，总共测试5轮来度量性能
                .forks(1)
                .threads(10)
                .result("result.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(opt).run();
    }

//    @Benchmark
//    @BenchmarkMode({Mode.AverageTime})
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void atomicLongIncrementTest() {
//        for (int i = 0; i < 100000; i++) {
//            ATOMIC_LONG_VALUE.incrementAndGet();
//        }
//    }
//
//    @Benchmark
//    @BenchmarkMode({Mode.AverageTime})
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    public void longAddrIncrementTest1() {
//        for (int i = 0; i < 100000; i++) {
//            LONG_ADDER_VALUE.add(1L);
//        }
//    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void longAddrIncrementTest2() {
        for (int i = 0; i < 100000; i++) {
            LONG_ADDER_VALUE2.add(1L);
        }
    }
}
