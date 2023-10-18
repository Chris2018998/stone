package org.stone.beetp.performance;

import java.util.concurrent.Callable;

public class CallTask implements Callable {
   public Object call() throws Exception{
       return  "Hello";
   }
}
