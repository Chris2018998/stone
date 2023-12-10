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

import java.util.concurrent.*;

@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
public class TaskPoolBenchmark {
    private static TaskService taskService;
    private static ForkJoinPool forkJoinPool;
    private static ThreadPoolExecutor executor;
    private static int count = 100;
    private static int[] numbers = new int[count];
    private static CallTask callTask = new CallTask();
    private static HelloTask helloTask = new HelloTask();

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(TaskPoolBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testOnceTaskJDK() throws Exception {
        Future future = executor.submit(callTask);
        future.get();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testForkJoinJDK() throws Exception {
        ForkJoinTask task = forkJoinPool.submit(new SumTask(numbers));
        task.get();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testOnceTaskBee() throws Exception {
        TaskHandle handle = taskService.submit(helloTask);
        handle.get();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testForkJoinBee() throws Exception {
        ArraySumComputeTask task = new ArraySumComputeTask(numbers);
        TaskHandle<Integer> joinHandle = taskService.submit(task, new ArraySumJoinOperator());
        int joinSum = joinHandle.get();
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        executor = new ThreadPoolExecutor(4, 4, 15, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100));
        executor.prestartAllCoreThreads();

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
        executor.shutdownNow();
        forkJoinPool.shutdownNow();
        taskService.terminate(true);
    }
}
