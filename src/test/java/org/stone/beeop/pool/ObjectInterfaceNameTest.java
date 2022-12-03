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
import org.stone.beeop.object.Book;
import org.stone.beeop.object.JavaBook;

/**
 * ObjectFactory subclass
 *
 * @author chris.liao
 */
public class ObjectInterfaceNameTest extends TestCase {
    private BeeObjectSource obs;

    public void setUp() throws Throwable {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectClass(JavaBook.class);
        config.setObjectInterfaceNames(new String[]{Book.class.getName()});
        obs = new BeeObjectSource(config);
    }

    public void tearDown() throws Throwable {
        obs.close();
    }

    public void test() throws Exception {
        BeeObjectHandle handle = null;
        try {
            handle = obs.getObject();
            if (handle == null)
                TestUtil.assertError("Failed to get object");
            Book book = (Book) handle.getObjectProxy();
            System.out.println("Book name:" + book.getName());
        } finally {
            if (handle != null)
                handle.close();
        }
    }
}
