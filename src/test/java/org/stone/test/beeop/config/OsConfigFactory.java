/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.config;

import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * Config Factory
 *
 * @author Chris Liao
 */

public class OsConfigFactory {

    public static BeeObjectSourceConfig<String, Book> createEmpty() {
        return new BeeObjectSourceConfig<>();
    }

    public static BeeObjectSourceConfig<String, Book> createDefault() {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setObjectFactory(new TextBookFactory());
        return config;
    }
}


