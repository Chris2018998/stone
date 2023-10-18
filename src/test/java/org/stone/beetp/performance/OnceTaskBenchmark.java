package org.stone.beetp.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.beetp.BeeTaskHandle;
import org.stone.beetp.BeeTaskService;
import org.stone.beetp.BeeTaskServiceConfig;

import java.util.concurrent.*;

@Threads(4)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 4, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OnceTaskBenchmark {
    private static BeeTaskService taskService;
    private static ThreadPoolExecutor executor;
    private  static CallTask callTask = new CallTask();
    private  static HelloTask helloTask = new HelloTask();

    static {
        BeeTaskServiceConfig config = new BeeTaskServiceConfig();
        config.setWorkInDaemon(true);
        config.setInitWorkerSize(4);
        config.setMaxWorkerSize(4);
        taskService = new BeeTaskService(config);
        executor = new ThreadPoolExecutor(4,4,15,TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ForkJoinBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testBeeTP() throws Exception {
        BeeTaskHandle handle = taskService.submit(helloTask);
        handle.get();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static void testJDKExecutor() throws Exception {
        Future future= executor.submit(callTask);
        future.get();
    }
}
