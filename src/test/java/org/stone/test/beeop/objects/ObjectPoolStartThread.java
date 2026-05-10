/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objects;

import org.junit.jupiter.api.Assertions;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.book.Book;

/**
 * Object Pool start thread.
 *
 * @author Chris Liao
 */
public class ObjectPoolStartThread extends BaseThread {
    public ObjectPoolStartThread(BeeObjectSourceConfig<String, Book> config) {
        this.config = config;
        this.setDaemon(true);
    }

    public ObjectPoolStartThread(BeeObjectSource<String, Book> objectSource) {
        this.objectSource = objectSource;
        this.setDaemon(true);
    }

    public void run() {
        if (this.config != null) {
            try (BeeObjectSource<String, Book> ignored = new BeeObjectSource<>(config)) {
                Assertions.fail("Object source pool startup failed");
            } catch (Exception e) {
                this.failureCause = e;
            }
        } else if (this.objectSource != null) {//lazy
            try (BeeObjectHandle<String, Book> ignored = objectSource.getObjectHandle()) {
                Assertions.fail("Object source pool startup failed");
            } catch (Exception e) {
                this.failureCause = e;
            }
        }
    }
}


