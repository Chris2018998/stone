package org.stone.beetp.performance.mock;

import org.stone.beetp.Task;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskService;

public class BeeOnceTaskSubmitThread extends TimeMonitorTaskSubmitThread {
    private final Task beeTask;
    private final TaskService taskService;

    public BeeOnceTaskSubmitThread(int loopCount, TaskService taskService, Task beeTask) {
        super(loopCount);
        this.beeTask = beeTask;
        this.taskService = taskService;
    }

    public void submitTask() {
        try {
            TaskHandle handle = taskService.submit(beeTask);
            handle.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}