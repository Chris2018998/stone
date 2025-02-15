/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * <p>
 * Copyright(C) Chris2018998,All rights reserved.
 * <p>
 * Project owner contact:Chris2018998@tom.com.
 * <p>
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.StoneLogAppender;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.objects.Book;
import org.stone.beeop.objects.BookMarket;

import static org.stone.base.TestUtil.getStoneLogAppender;

/**
 * @author Chris Liao
 */
public class Tc0010ConfigInfoLogPrintTest extends TestCase {

    public void testOnConfigPrintInd() {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();

        //situation1: not print config
        config.setPrintConfigInfo(false);//test point
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.isEmpty());

        //situation2: print config items
        config.setPrintConfigInfo(true);//test point
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.isEmpty());
    }

    public void testOnExclusionConfigItems() {
        StoneLogAppender logAppender = getStoneLogAppender();
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        config.setPrintConfigInfo(true);

        //situation1:
        logAppender.beginCollectStoneLog();
        config.check();
        String logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".initialSize"));
        Assert.assertTrue(logs.contains(".maxKeySize"));

        config.addConfigPrintExclusion("aliveTestTimeout");
        config.clearAllConfigPrintExclusion();
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".initialSize"));
        Assert.assertTrue(logs.contains(".maxKeySize"));

        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".initialSize"));
        Assert.assertTrue(logs.contains(".maxKeySize"));

        //situation2:
        config.addConfigPrintExclusion("initialSize");
        config.addConfigPrintExclusion("maxKeySize");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".initialSize"));
        Assert.assertFalse(logs.contains(".maxKeySize"));


        config.setObjectInterfaces(new Class[]{Book.class});
        config.setObjectInterfaceNames(new String[]{Book.class.getName()});
        config.addFactoryProperty("name", "Edition of Java world");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".objectInterfaces"));
        Assert.assertTrue(logs.contains(".objectInterfaceNames"));
        Assert.assertTrue(logs.contains(".factoryProperties"));

        config.setObjectInterfaces(new Class[]{Book.class, BookMarket.class});
        config.setObjectInterfaceNames(new String[]{Book.class.getName(), BookMarket.class.getName()});
        config.addFactoryProperty("name", "Edition of Java world");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertTrue(logs.contains(".objectInterfaces"));
        Assert.assertTrue(logs.contains(".objectInterfaceNames"));
        Assert.assertTrue(logs.contains(".factoryProperties"));


        config.addConfigPrintExclusion("objectInterfaces");
        config.addConfigPrintExclusion("objectInterfaceNames");
        config.addConfigPrintExclusion("factoryProperties");
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".objectInterfaces"));
        Assert.assertFalse(logs.contains(".objectInterfaceNames"));
        Assert.assertFalse(logs.contains(".factoryProperties"));

        config.setObjectInterfaces(new Class[0]);
        config.setObjectInterfaceNames(new String[0]);
        logAppender.beginCollectStoneLog();
        config.check();
        logs = logAppender.endCollectedStoneLog();
        Assert.assertFalse(logs.contains(".objectInterfaces"));
        Assert.assertFalse(logs.contains(".objectInterfaceNames"));
        Assert.assertFalse(logs.contains(".factoryProperties"));
    }
}
