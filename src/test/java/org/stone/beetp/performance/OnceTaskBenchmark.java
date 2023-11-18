package org.stone.beetp.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OnceTaskBenchmark {
    private static TaskService taskService;
    private static ThreadPoolExecutor executor;
    private static CallTask callTask = new CallTask();
    private static HelloTask helloTask = new HelloTask();

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(OnceTaskBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public static void testBeeTaskExecutor() throws Exception {
        TaskHandle handle = taskService.submit(helloTask);
        handle.get();
    }

    @Benchmark
    public static void testThreadPoolExecutor() throws Exception {
        Future future = executor.submit(callTask);
        future.get();
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        executor = new ThreadPoolExecutor(4, 4, 15, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        executor.prestartAllCoreThreads();

        TaskServiceConfig config = new TaskServiceConfig();
        config.setInitWorkerSize(4);
        config.setMaxWorkerSize(4);
        config.setWorkerKeepAliveTime(TimeUnit.SECONDS.toMillis(15));
        taskService = new TaskService(config);
    }

    @TearDown
    public void teardown() throws Exception {
        executor.shutdownNow();
        taskService.terminate(true);
    }
}
