package org.stone.test.study;

import org.stone.beeop.pool.ObjectPoolStatics;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import static org.stone.tools.CommonUtil.isBlank;

public class MethodHandleTest {
    private static final MethodHandleTest bean = new MethodHandleTest();
    private static final Class<?> beanClass = MethodHandleTest.class;
    private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

    public static Object call(String methodName) throws Throwable {
        return call(methodName, ObjectPoolStatics.EMPTY_CLASSES, null);
    }

    public static Object call(String methodName, Class<?>[] parameterTypes, Object[] parameterValues) throws Throwable {
        if (isBlank(methodName)) throw new IllegalArgumentException("Method name can't be null or be blank");
        if (parameterTypes == null) throw new IllegalArgumentException("Method parameter types cannot be null");

        MethodHandle methodHandle = null;
        if (methodHandle == null) {
            Method targetMethod = beanClass.getMethod(methodName, parameterTypes);
            methodHandle = lookup.findVirtual(beanClass, methodName, MethodType.methodType(targetMethod.getReturnType(), parameterTypes));
        }

        int parameterLen = parameterTypes.length;
        Object[] invokeParameters = new Object[parameterLen + 1];
        invokeParameters[0] = bean;
        if (parameterValues != null && parameterValues.length > 0) {
            int copyLen = Math.min(parameterLen, parameterValues.length);
            System.arraycopy(parameterValues, 0, invokeParameters, 1, copyLen);
        }
        return methodHandle.invokeWithArguments(invokeParameters);
    }

    public static void main(String[] ars) throws Throwable {
        System.out.println(call("printHello"));
        System.out.println(call("printHello", new Class[]{}, null));
        System.out.println(call("printHello", new Class[]{String.class}, new Object[]{"china"}));
    }

    public void printHello() {
        System.out.println("Hello,");
    }

    public void printHello(String name) {
        System.out.println("Hello," + name);
    }
}
