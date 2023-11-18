package org.stone.beetp.performance;

import org.stone.beetp.Task;

public class HelloTask implements Task {
    public Object call() {
        return "Hello";
    }
}
