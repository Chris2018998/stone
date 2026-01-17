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

import org.stone.beeop.*;

import java.util.List;

public class MockBlockPoolImplementation<K, V> implements BeeObjectPool<K, V> {

    public BeeObjectHandle<K, V> getObjectHandle() throws Exception {
        return null;
    }

    public BeeObjectHandle<K, V> getObjectHandle(K key) throws Exception {
        return null;
    }

    public int keySize() {
        return 0;
    }

    public boolean existsKey(K key) throws Exception {
        return false;
    }

    public boolean suspendKey(K key) {
        return true;
    }

    public boolean resumeKey(K key) {
        return true;
    }

    public void clearKeyObjects(K key) {
    }

    public void clearKeyObjects(K key, boolean forceRecycleBorrowed) {
    }

    public boolean deleteKey(K key) {
        return true;
    }

    public boolean deleteKey(K key, boolean forceRecycleBorrowed) {
        return true;
    }

    public void close() {
    }

    public boolean isClosed() {
        return false;
    }

    public boolean suspendPool() {
        return true;
    }

    public boolean resumePool() {
        return true;
    }

    public void start(BeeObjectSourceConfig<K, V> config) throws Exception {
    }

    public void restart(boolean forceRecycleBorrowed) {
    }

    public void restart(boolean forceRecycleBorrowed, BeeObjectSourceConfig<K, V> config) {
    }

    public void enableLogPrinter(boolean enable) {
    }

    public void enableLogPrinter(K key, boolean enable) {
    }

    public BeeObjectPoolMonitorVo<K> getPoolMonitorVo(boolean includeKeys) throws Exception {
        return null;
    }

    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) {
        return null;
    }


    public List<Thread> interruptWaitingThreads() throws Exception {
        return null;
    }

    public List<Thread> interruptWaitingThreads(K key) throws Exception {
        return null;
    }

    public void enableLogCache(boolean enable) throws Exception {

    }

    public void changeLogListener(BeeMethodLogListener<K> listener) throws Exception {

    }

    public void clearPoolLogs() throws Exception {
        //@todo
    }

    public List<BeeMethodLog<K>> getPoolLogs() throws Exception {
        return null;//@todo
    }

    public void clearKeyLogs(K key) throws Exception {
        //@todo
    }

    public List<BeeMethodLog<K>> getKeyLogs(K key) throws Exception {
        return null;//@todo
    }

    public void clearKeyedObjectCallLogs(K key) throws Exception {
        //@todo
    }

    public List<BeeMethodLog<K>> getKeyedObjectCallLogs(K key) throws Exception {
        return null;//@todo
    }
}
