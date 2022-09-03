/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop.pool;

import org.jmin.beeop.*;
import org.jmin.beeop.object.Book;
import org.jmin.beeop.object.JavaBook;

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
