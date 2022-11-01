/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beeop.object;

import org.stone.beeop.RawObjectFactory;

/**
 * ObjectFactory subclass
 *
 * @author chris.liao
 */
public class JavaBookFactory implements RawObjectFactory {
    public Object create() throws Exception {
        return new JavaBook("Java核心技术·卷1", System.currentTimeMillis());
    }

    public void setDefault(Object obj) throws Exception {
        //do nothing
    }

    public void reset(Object obj) throws Exception {
        //do nothing
    }

    public void destroy(Object obj) {
        //do nothing
    }

    public boolean isValid(Object obj, int timeout) {
        return true;
    }
}
