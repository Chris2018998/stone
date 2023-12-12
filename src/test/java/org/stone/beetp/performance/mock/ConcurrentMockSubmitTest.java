package org.stone.beetp.performance.mock;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConcurrentMockSubmitTest {
    private static LinkedHashMap factoryClassNameMap = new LinkedHashMap(4);

    static {
        factoryClassNameMap.put("JDKOnceTask", "org.stone.beetp.performance.mock.JDKOnceTaskSubmitThreadsFactory");
        factoryClassNameMap.put("BeeOnceTask", "org.stone.beetp.performance.mock.BeeOnceTaskSubmitThreadsFactory");
        factoryClassNameMap.put("JDKJoinTask", "org.stone.beetp.performance.mock.JDKJoinTaskSubmitThreadsFactory");
        factoryClassNameMap.put("BeeJoinTask", "org.stone.beetp.performance.mock.BeeJoinTaskSubmitThreadsFactory");
    }

    private static TimeMonitorTaskPoolInitConfig creatConfig() {
        TimeMonitorTaskPoolInitConfig config = new TimeMonitorTaskPoolInitConfig();
        config.setInitWorkerSize(4);
        config.setMaxWorkerSize(4);
        config.setMaxTaskSize(Integer.MAX_VALUE);
        config.setKeepAliveTime(15);
        config.setKeepAliveTimeUnit(TimeUnit.SECONDS);

        //producer
        config.setSubmitThreadSize(100);
        config.setSubmitCountPerThread(1000);
        return config;
    }

    public static void main(String[] args) {
        TimeMonitorTaskPoolInitConfig config = creatConfig();
        Iterator<Map.Entry<String, String>> itor = factoryClassNameMap.entrySet().iterator();
        while (itor.hasNext()) {
            Map.Entry<String, String> entry = itor.next();
            String name = entry.getKey();
            String className = entry.getValue();

            try {
                TimeMonitorTaskThreadsFactory factory = (TimeMonitorTaskThreadsFactory) Class.forName(className).newInstance();
                TimeMonitorTaskSubmitThread[] threads = factory.create(config);
                for (TimeMonitorTaskSubmitThread thread : threads) {
                    thread.start();
                }
                for (TimeMonitorTaskSubmitThread thread : threads) {
                    thread.join();
                }

                long totalTime = 0L;
                for (TimeMonitorTaskSubmitThread thread : threads) {
                    for (long time : thread.getTaskTookTime()) {
                        totalTime = totalTime + time;
                    }
                }

                int taskCount = config.getSubmitCountPerThread() * config.getSubmitThreadSize();
                long avgTime = totalTime / taskCount;

                System.out.println("[" + name + "] --- 执行任务总数：" + taskCount + ",消耗总时间：" + totalTime + "毫秒，平均时间：" + avgTime + "毫秒");
                factory.shutdownTaskPool();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
