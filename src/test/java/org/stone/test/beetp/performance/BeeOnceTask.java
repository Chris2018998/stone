package org.stone.test.beetp.performance;

import org.stone.beetp.Task;

public class BeeOnceTask implements Task {
    public Object call() {
        return "Hello";
    }
}
