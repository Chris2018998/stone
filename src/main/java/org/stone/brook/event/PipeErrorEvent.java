/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.event;

import org.stone.brook.Pipe;

/**
 * When exception occurs during net operation
 *
 * @author Chris
 */

public class PipeErrorEvent extends PipeEvent {

    /**
     * caused exception
     */
    public Throwable detail;

    /**
     * Constructor with a source object.
     */
    public PipeErrorEvent(Pipe source, Throwable detail) {
        super(source);
        this.detail = detail;
    }

    /**
     * Return caused throwable.
     */
    public Throwable getCause() {
        return detail;
    }

    /**
     * override method
     *
     * @return
     */
    public String toString() {
        return detail.toString();
    }
}