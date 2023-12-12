package org.stone.beetp.performance.mock;

import java.util.concurrent.TimeUnit;

public class TimeMonitorTaskPoolInitConfig {

    private int submitThreadSize;//task producer size

    private int submitCountPerThread;//task size submitted to pool per thread

    private int initWorkerSize;

    private int maxWorkerSize;

    private int maxTaskSize;

    private long keepAliveTime;

    private TimeUnit keepAliveTimeUnit;


    public int getSubmitThreadSize() {
        return submitThreadSize;
    }

    public void setSubmitThreadSize(int submitThreadSize) {
        this.submitThreadSize = submitThreadSize;
    }

    public int getSubmitCountPerThread() {
        return submitCountPerThread;
    }

    public void setSubmitCountPerThread(int submitCountPerThread) {
        this.submitCountPerThread = submitCountPerThread;
    }

    public int getInitWorkerSize() {
        return initWorkerSize;
    }

    public void setInitWorkerSize(int initWorkerSize) {
        this.initWorkerSize = initWorkerSize;
    }

    public int getMaxWorkerSize() {
        return maxWorkerSize;
    }

    public void setMaxWorkerSize(int maxWorkerSize) {
        this.maxWorkerSize = maxWorkerSize;
    }

    public int getMaxTaskSize() {
        return maxTaskSize;
    }

    public void setMaxTaskSize(int maxTaskSize) {
        this.maxTaskSize = maxTaskSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public TimeUnit getKeepAliveTimeUnit() {
        return keepAliveTimeUnit;
    }

    public void setKeepAliveTimeUnit(TimeUnit keepAliveTimeUnit) {
        this.keepAliveTimeUnit = keepAliveTimeUnit;
    }
}
