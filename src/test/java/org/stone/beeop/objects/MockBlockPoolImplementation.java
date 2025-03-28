/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.objects;

import org.stone.beeop.BeeKeyedObjectPool;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;

public class MockBlockPoolImplementation implements BeeKeyedObjectPool {
    public void init(BeeObjectSourceConfig config) throws Exception {
    }

    public void close() {
    }

    public boolean isClosed() {
        return false;
    }

    public void setPrintRuntimeLog(boolean indicator) {
    }

    public BeeObjectPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    public void clear(boolean forceCloseUsing) throws Exception {
    }

    public void clear(boolean forceCloseUsing, BeeObjectSourceConfig config) throws Exception {
    }

    public BeeObjectHandle getObjectHandle() {
        return new MockObjectHandleImpl();
    }

    public BeeObjectHandle getObjectHandle(Object key) {
        return new MockObjectHandleImpl();
    }

    public int getObjectCreatingCount(Object key) {
        return 0;
    }

    public int getObjectCreatingTimeoutCount(Object key) {
        return 0;
    }

    public Thread[] interruptObjectCreating(Object key, boolean onlyInterruptTimeout) {
        return null;
    }

    public boolean isPrintRuntimeLog(Object key) {
        return false;
    }

    public void setPrintRuntimeLog(Object key, boolean indicator) throws Exception {
    }

    public BeeObjectPoolMonitorVo getMonitorVo(Object key) {
        return null;
    }

    public int keySize() {
        return 1;
    }

    public Object[] keys() {
        return null;
    }

    public boolean exists(Object key) {
        return true;
    }

    public void clear(Object key) {
    }

    public void clear(Object key, boolean forceCloseUsing) {
    }

    public void deleteKey(Object key) {
    }

    public void deleteKey(Object key, boolean forceCloseUsing) {
    }
}
