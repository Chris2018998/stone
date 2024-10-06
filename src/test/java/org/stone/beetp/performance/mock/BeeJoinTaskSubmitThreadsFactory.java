package org.stone.beetp.performance.mock;

import org.stone.beetp.Task;
import org.stone.beetp.TaskJoinOperator;
import org.stone.beetp.TaskService;
import org.stone.beetp.TaskServiceConfig;
import org.stone.beetp.performance.BeeJoinTask;
import org.stone.beetp.performance.BeeJoinTaskOpr;

public class BeeJoinTaskSubmitThreadsFactory implements TimeMonitorTaskThreadsFactory {
    private TaskService taskService;

    public TimeMonitorTaskSubmitThread[] create(TimeMonitorTaskPoolInitConfig config) {
        TaskServiceConfig serverConfig = new TaskServiceConfig();
        serverConfig.setMaxTaskSize(config.getMaxTaskSize());
        serverConfig.setWorkerKeepAliveTime(config.getKeepAliveTimeUnit().toMillis(15));
        this.taskService = new TaskService(serverConfig);

        int submitThreadSize = config.getSubmitThreadSize();
        int submitTaskCount = config.getSubmitCountPerThread();
        BeeJoinTaskSubmitThread[] threads = new BeeJoinTaskSubmitThread[submitThreadSize];

        int count = 100;
        int[] numbers = new int[count];
        for (int i = 0; i < count; i++)
            numbers[i] = i;
        Task beeTask = new BeeJoinTask(numbers);
        TaskJoinOperator operator = new BeeJoinTaskOpr();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new BeeJoinTaskSubmitThread(submitTaskCount, taskService, beeTask, operator);

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
