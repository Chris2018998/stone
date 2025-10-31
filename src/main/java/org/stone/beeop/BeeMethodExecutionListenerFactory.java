/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * Method execution listener interface factory
 *
 * @author Chris Liao
 */
public interface BeeMethodExecutionListenerFactory<K, V> {

    /**
     * Create method execution listener.
     *
     * @return created Listener instance
     */
    BeeMethodExecutionListener<K, V> create(BeeObjectSourceConfig<K, V> config) throws Exception;
}


