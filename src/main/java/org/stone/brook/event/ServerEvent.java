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

import java.util.EventObject;

/**
 * Socket server事件
 *
 * @author Chris Liao
 */

public class ServerEvent extends EventObject {

    /**
     * Server port
     */
    private int serverPort;

    /**
     * Event constructor
     */
    public ServerEvent(int serverPort) {
        super("Localhost");
        this.serverPort = serverPort;
    }

    /**
     * Return port
     */
    public int getPort() {
        return this.serverPort;
    }

    /**
     * Hash code
     */
    public int hashCode() {
        return this.serverPort;
    }

    /**
     * equals method
     */
    public boolean equals(Object obj) {
        if (obj instanceof ServerEvent) {
            ServerEvent other = (ServerEvent) obj;
            return (this.serverPort == other.serverPort);
        } else {
            return false;
        }
    }

    /**
     * override method
     */
    public String toString() {
        return "Localhost:" + serverPort;
    }
}
