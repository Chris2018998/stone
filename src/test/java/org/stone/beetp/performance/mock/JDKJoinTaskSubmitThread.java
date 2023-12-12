package org.stone.beetp.performance.mock;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class JDKJoinTaskSubmitThread extends TimeMonitorTaskSubmitThread {
    private final RecursiveTask jdkTask;
    private final ForkJoinPool forkJoinPool;

    public JDKJoinTaskSubmitThread(int loopCount, ForkJoinPool forkJoinPool, RecursiveTask jdkTask) {
        super(loopCount);
        this.jdkTask = jdkTask;
        this.forkJoinPool = forkJoinPool;
    }

    public void submitTask() {
        try {
            ForkJoinTask task = forkJoinPool.submit(jdkTask);
            task.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}