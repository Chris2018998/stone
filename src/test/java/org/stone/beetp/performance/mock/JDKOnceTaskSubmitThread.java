package org.stone.beetp.performance.mock;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class JDKOnceTaskSubmitThread extends TimeMonitorTaskSubmitThread {
    private final Callable jdkTask;
    private final ThreadPoolExecutor executor;

    public JDKOnceTaskSubmitThread(int loopCount, ThreadPoolExecutor executor, Callable jdkTask) {
        super(loopCount);
        this.jdkTask = jdkTask;
        this.executor = executor;
    }

    public void submitTask() {
        try {
            //System.out.println("submit JDKOnce");
            Future handle = executor.submit(jdkTask);
            handle.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}