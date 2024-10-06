package org.stone.beetp.performance.mock;

import org.stone.beetp.TaskJoinOperator;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.performance.BeeJoinTaskOpr;
import org.stone.beetp.performance.BeeOnceTask;

public class BeeOnceTaskSubmitThreadsFactory implements TimeMonitorTaskThreadsFactory {
    private TaskService taskService;

    public TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config) {
        TaskServiceConfig serverConfig = new TaskServiceConfig();
        serverConfig.setMaxTaskSize(config.getMaxTaskSize());
        serverConfig.setWorkerKeepAliveTime(config.getKeepAliveTimeUnit().toMillis(15));
        this.taskService = new TaskService(serverConfig);

        int submitThreadSize = config.getSubmitThreadSize();
        int submitTaskCount = config.getSubmitCountPerThread();
        BeeOnceTaskSubmitThread[] threads = new BeeOnceTaskSubmitThread[submitThreadSize];

        BeeOnceTask beeTask = new BeeOnceTask();
        TaskJoinOperator operator = new BeeJoinTaskOpr();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new BeeOnceTaskSubmitThread(submitTaskCount, taskService, beeTask);
        }

        return threads;
    }

    public void shutdownTaskPool() {
        try {
            taskService.shutdown(true);
        } catch (Exception e) {

        }
    }
}
