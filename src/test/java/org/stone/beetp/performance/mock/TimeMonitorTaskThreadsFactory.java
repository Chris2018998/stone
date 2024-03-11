package org.stone.beetp.performance.mock;

public interface TimeMonitorTaskThreadsFactory {

    TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config);

    void shutdownTaskPool();
}
