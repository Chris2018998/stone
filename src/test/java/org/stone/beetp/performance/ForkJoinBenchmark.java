package org.stone.beetp.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.join.ArraySumComputeTask;
import org.stone.beetp.join.ArraySumJoinOperator;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
public class ForkJoinBenchmark {
    private static TaskService taskService;
    private static ForkJoinPool forkJoinPool;
    private static int count = 100;
    private static int[] numbers = new int[count];

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ForkJoinBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testBeeTP() throws Exception {
        ArraySumComputeTask task = new ArraySumComputeTask(numbers);
        TaskHandle<Integer> joinHandle = taskService.submit(task, new ArraySumJoinOperator());
        int joinSum = joinHandle.get();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testJDKForkJoin() throws Exception {
        ForkJoinTask task = forkJoinPool.submit(new SumTask(numbers));
        task.get();
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        for (int i = 0; i < count; i++)
            numbers[i] = i;

        forkJoinPool = new ForkJoinPool();
        TaskServiceConfig config = new TaskServiceConfig();
        config.setInitWorkerSize(4);
        config.setMaxWorkerSize(4);
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(15));
        taskService = new TaskService(config);
    }

    @TearDown
    public void teardown() throws Exception {
        forkJoinPool.shutdownNow();
        taskService.terminate(true);
    }
}
