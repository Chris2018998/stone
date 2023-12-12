package org.stone.beetp.performance.mock;

import org.stone.beetp.performance.JDKJoinTask;

import java.util.concurrent.ForkJoinPool;

public class JDKJoinTaskSubmitThreadsFactory implements TimeMonitorTaskThreadsFactory {
    private static ForkJoinPool forkJoinPool;

    public TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config) {
        forkJoinPool = new ForkJoinPool();

        int submitThreadSize = config.getSubmitThreadSize();
        int submitTaskCount = config.getSubmitCountPerThread();
        JDKJoinTaskSubmitThread[] threads = new JDKJoinTaskSubmitThread[submitThreadSize];

        int count = 100;
        int[] numbers = new int[count];
        for (int i = 0; i < count; i++)
            numbers[i] = i;
        JDKJoinTask task = new JDKJoinTask(numbers);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new JDKJoinTaskSubmitThread(submitTaskCount, forkJoinPool, task);
        }

        return threads;
    }

    public void shutdownTaskPool() {
        try {
            forkJoinPool.shutdownNow();
        } catch (Exception e) {
        }
    }
}
