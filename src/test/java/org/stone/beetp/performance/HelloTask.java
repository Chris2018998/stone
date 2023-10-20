package org.stone.beetp.performance;

import org.stone.beetp.BeeTask;

public class HelloTask implements BeeTask {
    public Object call() {
        return "Hello";
    }
}
