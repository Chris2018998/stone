package org.stone.beetp.performance.mock;

public interface TimeMonitorTaskThreadsFactory {

    public TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config);

    public void shutdownTaskPool();
}
