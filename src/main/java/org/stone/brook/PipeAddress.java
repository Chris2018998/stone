/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook;

/**
 * We use this class to describe address for communication host
 *
 * @author Chris Liao
 * @version 1.0
 */

public class PipeAddress {

    /**
     * host IP
     */
    private String hostIP;

    /**
     * host name
     */
    private String hostName;

    /**
     * 端口
     */
    private int hostPort;


    /**
     * Constructor
     */
    public PipeAddress(String hostIP, String hostName, int hostPort) {
        this.hostIP = hostIP;
        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    /**
     * Return host IP
     */
    public String getHostIP() {
        return hostIP;
    }

    /**
     * Return host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Return port
     */
    public int getHostPort() {
        return hostPort;
    }

    /**
     * override method
     */
    public String toString() {
        return hostIP + "(" + hostName + "):" + hostPort;
    }

    /**
     * override method
     */
    public int hashCode() {
        return hostIP.hashCode() ^ hostPort;
    }

    /**
     * override method
     */
    public boolean equals(Object obj) {
        if (obj instanceof PipeAddress) {
            PipeAddress other = (PipeAddress) obj;
            return this.hostIP.equals(other.hostIP) && this.hostPort == other.hostPort;
        } else {
            return false;
        }
    }
}