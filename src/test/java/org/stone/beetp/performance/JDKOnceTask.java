package org.stone.beetp.performance;

import java.util.concurrent.Callable;

public class JDKOnceTask implements Callable {
    public Object call() throws Exception {
        return "Hello";
    }
}
