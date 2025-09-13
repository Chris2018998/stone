package org.stone.test.study;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class VarHandleTest {
    private Integer age;

    public static void main(String[] ars) throws Exception {
        MethodHandles.Lookup l = MethodHandles.lookup();
        VarHandle handle = l.findVarHandle(VarHandleTest.class, "age", Integer.class);

        VarHandleTest test = new VarHandleTest();
        VarHandleTest test2 = new VarHandleTest();

        if (handle.compareAndSet(test, null, 1)) {
            System.out.println("compareAndSet-success:" + handle.get(test));
        }

        //1:compareAndSet return success or fail
        //2:compareAndExchange return current value,don't care success or fail
        System.out.println("compareAndExchange(1->2):" + handle.compareAndExchange(test, 1, 2));
        System.out.println("compareAndExchange(2->3):" + handle.compareAndExchange(test, 2, 3));
        System.out.println("compareAndExchange(2->3):" + handle.compareAndExchange(test, 2, 3));

        System.out.println("compareAndExchangeAcquire(3->4):" + handle.compareAndExchangeAcquire(test, 3, 4));//current is 3
        System.out.println("compareAndExchangeAcquire(4->5):" + handle.compareAndExchangeAcquire(test, 4, 5));//current is 4

        System.out.println("compareAndExchangeRelease(5->6):" + handle.compareAndExchangeRelease(test, 5, 6));//current is 3
        System.out.println("compareAndExchangeRelease(6->7):" + handle.compareAndExchangeRelease(test, 6, 7));//current is 4
    }
}
