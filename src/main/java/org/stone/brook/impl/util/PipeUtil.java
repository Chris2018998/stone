/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.brook.impl.util;

import org.stone.brook.Pipe;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Util class
 *
 * @author Chris
 */

public class PipeUtil {

    /**
     * A null string ?
     */
    public static boolean isNull(String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * get hash Code by array
     */
    public static double getJavaVersion() {
        return Double.parseDouble(System.getProperty("java.version").substring(0, 3));
    }

    /**
     * close Connection
     */
    public static void close(Pipe pipe) {
        try {
            if (pipe != null)
                pipe.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close InputStreamt
     */
    public static void close(InputStream inputStream) {
        try {
            if (inputStream != null)
                inputStream.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close outputStream
     */
    public static void close(OutputStream outputStream) {
        try {
            if (outputStream != null)
                outputStream.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close writer
     */
    public static void close(Writer writer) {
        try {
            if (writer != null)
                writer.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close Reader
     */
    public static void close(Reader reader) {
        try {
            if (reader != null)
                reader.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close Socket
     */
    public static void close(Socket socket) {
        try {
            if (socket != null)
                socket.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close ServerSocket
     */
    public static void close(ServerSocket socket) {
        try {
            if (socket != null)
                socket.close();
        } catch (Throwable e) {
        }
    }

    /**
     * close DatagramSocket
     */
    public static void close(DatagramSocket socket) {
        try {
            if (socket != null)
                socket.close();
        } catch (Throwable e) {
        }
    }
}
