/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.pool;

import org.stone.base.TestCase;
import org.stone.base.TestUtil;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.RawObjectMethodFilter;
import org.stone.beeop.object.Book;
import org.stone.beeop.object.JavaBookFactory;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class ObjectMethodIllegalAccessTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactoryClassName(JavaBookFactory.class.getName());
        config.setObjectInterfaces(new Class[]{Book.class});
        config.setObjectMethodFilter(new ExcludeMethodFilter());
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            handle = obs.getObjectHandle();
            test1(handle);
            test2(handle);
        } finally {
            if (handle != null)
                handle.close();
        }
    }

    public void test1(BeeObjectHandle handle) throws Exception {
        try {
            handle.call("getName", new Class[0], new Object[0]);
            TestUtil.assertError("Object method illegal access test fail");
        } catch (Exception e) {
            System.out.println("Handle method illegal access test OK");
        }
    }

    public void test2(BeeObjectHandle handle) throws Exception {
        try {
            Book book = (Book) handle.getObjectProxy();
            System.out.println(book.getName());
            TestUtil.assertError("Proxy method illegal access test fail");
        } catch (UndeclaredThrowableException e) {
            if (e.getCause() instanceof Exception)
                System.out.println("Proxy method illegal access test OK");
            else
                TestUtil.assertError("Proxy method illegal access test fail");
        }
    }

    class ExcludeMethodFilter implements RawObjectMethodFilter {
        public void doFilter(Object key, String methodName, Class[] paramTypes, Object[] paramValues) throws Exception {
            if ("getName".equals(methodName)) throw new IllegalAccessException();
        }
    }
}
