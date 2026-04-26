/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objectsource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.AliveTestFactory;

/**
 * @author Chris Liao
 */
public class Tc0065ObjectResetTest {

    @Test
    public void testReset() {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(1);
        config.setMaxActive(1);
        config.setMaxWait(1L);
        AliveTestFactory bookFactory = new AliveTestFactory("Java Concurrent", "DougLee");
        config.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                handle.call("setAuthor", new Class[]{String.class}, new Object[]{"Professor DougLee"});
                handle.call("setTitle", new Class[]{String.class}, new Object[]{"Java2 Concurrent"});

                Assertions.assertEquals("Professor DougLee", handle.call("getAuthor"));
                Assertions.assertEquals("Java2 Concurrent", handle.call("getTitle"));
            } catch (Throwable e) {
                Assertions.fail("[Tc0065ObjectResetTest.testReset]failed");
            }

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertEquals("DougLee", handle.call("getAuthor"));
                Assertions.assertEquals("Java Concurrent", handle.call("getTitle"));
            } catch (Throwable e) {
                Assertions.fail("[Tc0065ObjectResetTest.testReset]failed");
            }
        }
    }
}
