package org.stone.shine.concurrent.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.shine.util.concurrent.synchronizer.SyncVisitConfig;
import org.stone.shine.util.concurrent.synchronizer.base.ResultCall;
import org.stone.shine.util.concurrent.synchronizer.base.ResultWaitPool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.stone.shine.util.concurrent.synchronizer.SyncNodeStates.RUNNING;

@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ResultWaitPoolBenchmark {
    private static ResultWaitPool pool;
    private static AtomicResultCall call;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ReentrantLockBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testLock() throws Exception {
        SyncVisitConfig config = new SyncVisitConfig();
        config.allowInterruption(false);
        if (Boolean.TRUE.equals(pool.get(call, null, config))) {
            try {
                //do nothing
            } finally {
                if (call.incr() > 5) System.out.println("Atomic Int is greater than 5");
                pool.wakeupFirst(null);
            }
        }
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        call = new AtomicResultCall();
        pool = new ResultWaitPool();
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        System.gc();
    }

    private static class AtomicResultCall implements ResultCall {
        private AtomicInteger count = new AtomicInteger(5);

        int incr() {
            return count.incrementAndGet();
        }

        //decr
        public Object call(Object arg) throws Exception {
            while (true) {
                int current = count.get();
                if (current == 0) return Boolean.FALSE;
                if (count.compareAndSet(current, current - 1)) {
                    return Boolean.TRUE;
                }
            }
        }
    }
}

