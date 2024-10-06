package org.stone.beetp.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskJoinOperator;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.join.ArraySumComputeTask;
import org.stone.beetp.join.ArraySumJoinOperator;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TaskPoolBenchmark {
    private static final int count = 100;
    private static final int[] numbers = new int[count];
    private static final JDKJoinTask jdkJoinTask = new JDKJoinTask(numbers);
    private static final ArraySumComputeTask beeJoinTask = new ArraySumComputeTask(numbers);
    private static final JDKOnceTask callTask = new JDKOnceTask();
    private static final BeeOnceTask helloTask = new BeeOnceTask();
    private static final TaskJoinOperator beeJoinOperator = new ArraySumJoinOperator();
    private static TaskService taskService;
    private static ForkJoinPool forkJoinPool;
    private static ThreadPoolExecutor executor;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(TaskPoolBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void testForkJoinJDK() throws Exception {
//        ForkJoinTask task = forkJoinPool.submit(jdkJoinTask);
//        task.get();
//    }
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void testOnceTaskJDK() throws Exception {
//        Future future = executor.submit(callTask);
//        future.get();
//    }


//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.INLINE)
//    public static void testForkJoinBee() throws Exception {
//        TaskHandle<Integer> joinHandle = taskService.submit(beeJoinTask, beeJoinOperator);
//        joinHandle.get();
//    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testOnceTaskBee() throws Exception {
        TaskHandle handle = taskService.submit(helloTask);
        handle.get();
    }

    @Setup(Level.Trial)
    public static void setup(BenchmarkParams params) {
        executor = new ThreadPoolExecutor(4, 4, 15, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100));
        executor.prestartAllCoreThreads();

        for (int i = 0; i < count; i++)
            numbers[i] = i;

        forkJoinPool = new ForkJoinPool();
        TaskServiceConfig config = new TaskServiceConfig();
        config.setMaxTaskSize(1000);
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(15));
        taskService = new TaskService(config);
    }

    @TearDown(Level.Trial)
    public static void teardown() throws Exception {
        executor.shutdownNow();
        forkJoinPool.shutdownNow();
        taskService.shutdown(true);
    }
}
