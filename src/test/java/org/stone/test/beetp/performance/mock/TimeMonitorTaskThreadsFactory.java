package org.stone.test.beetp.performance.mock;

public interface TimeMonitorTaskThreadsFactory {

    TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config);

    void shutdownTaskPool();
}
