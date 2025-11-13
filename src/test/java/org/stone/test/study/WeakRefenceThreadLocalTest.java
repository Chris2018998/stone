package org.stone.test.study;

import java.lang.ref.WeakReference;

public class WeakRefenceThreadLocalTest {
    public static void main(String[] args) {
        BorrowerThreadLocal threadLocal = new BorrowerThreadLocal();
        threadLocal.set(new WeakReference<>(new Object()));
        Object cachedObject1 = threadLocal.get().get();

        cachedObject1 = null;
        System.gc();
        Object cachedObject22 = threadLocal.get().get();
        System.out.println("<testThreadLocal>--after call gc():" + cachedObject22);
    }

    private static final class BorrowerThreadLocal extends ThreadLocal<WeakReference<Object>> {
        BorrowerThreadLocal() {
        }

        protected WeakReference<Object> initialValue() {
            return new WeakReference<>(new Object());
        }
    }
}
