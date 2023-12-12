package org.stone.beetp.performance.mock;

public abstract class TimeMonitorTaskSubmitThread extends Thread {
    private final int submitSize;
    private final long[] taskTime;

    public TimeMonitorTaskSubmitThread(int submitSize) {
        this.submitSize = submitSize;
        this.taskTime = new long[submitSize];
    }

    public abstract void submitTask();

    public long[] getTaskTookTime() {
        return taskTime;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < submitSize; i++) {
            submitTask();
        }
        taskTime[0] = System.currentTimeMillis() - startTime;
    }
}
