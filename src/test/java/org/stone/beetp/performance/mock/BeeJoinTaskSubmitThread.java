package org.stone.beetp.performance.mock;

import org.stone.beetp.Task;
import org.stone.beetp.TaskHandle;
import org.stone.beetp.TaskJoinOperator;
import org.stone.beetp.TaskService;

public class BeeJoinTaskSubmitThread extends TimeMonitorTaskSubmitThread {
    private final Task beeTask;
    private final TaskJoinOperator operator;
    private final TaskService taskService;

    public BeeJoinTaskSubmitThread(int loopCount, TaskService taskService, Task beeTask, TaskJoinOperator operator) {
        super(loopCount);
        this.beeTask = beeTask;
        this.operator = operator;
        this.taskService = taskService;
    }

    public void submitTask() {
        try {
            TaskHandle handle = taskService.submit(beeTask, operator);
            handle.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}