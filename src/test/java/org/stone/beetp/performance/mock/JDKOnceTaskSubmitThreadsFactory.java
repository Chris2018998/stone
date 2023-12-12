package org.stone.beetp.performance.mock;

import org.stone.beetp.performance.JDKOnceTask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class JDKOnceTaskSubmitThreadsFactory implements TimeMonitorTaskThreadsFactory {
    private ThreadPoolExecutor executor;

    public TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config) {
        executor = new ThreadPoolExecutor(config.getInitWorkerSize(), config.getMaxWorkerSize(),
                config.getKeepAliveTime(), config.getKeepAliveTimeUnit(), new LinkedBlockingQueue<Runnable>(config.getMaxTaskSize()));
        executor.prestartAllCoreThreads();

        int submitThreadSize = config.getSubmitThreadSize();
        int submitTaskCount = config.getSubmitCountPerThread();
        JDKOnceTaskSubmitThread[] threads = new JDKOnceTaskSubmitThread[submitThreadSize];

        JDKOnceTask task = new JDKOnceTask();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new JDKOnceTaskSubmitThread(submitTaskCount, executor, task);
        }

        return threads;
    }

    public void shutdownTaskPool() {
        try {
            executor.shutdownNow();
        } catch (Exception e) {
        }
    }
}
