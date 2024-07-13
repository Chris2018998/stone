/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.object;

import org.stone.beeop.BeeObjectFactory;

/**
 * ObjectFactory subclass
 *
 * @author chris.liao
 */
public class JavaBookFactory implements BeeObjectFactory {

    public Object getInitialKey() {
        return new Object();
    }


    public Object getDefaultKey() {
        return new Object();
    }

    public Object create(Object key) throws Exception {
        return new JavaBook("Java核心技术·卷1", System.currentTimeMillis());
    }

    public void setDefault(Object key, Object obj) throws Exception {
        //do nothing
    }

    public void reset(Object key, Object obj) throws Exception {
        //do nothing
    }

    public void destroy(Object key, Object obj) {
        //do nothing
    }

    public boolean isValid(Object key, Object obj, int timeout) {
        return true;
    }
}
