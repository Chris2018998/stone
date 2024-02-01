/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beecp.other.slowSql;

import org.stone.beecp.BeeDataSource;
import org.stone.shine.util.concurrent.CountDownLatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class VisitThread extends Thread {
    private long timePoint;
    private BeeDataSource ds;
    private CountDownLatch latch;


    public VisitThread(BeeDataSource ds, CountDownLatch latch, long timePoint) {
        this.ds = ds;
        this.latch = latch;
        this.timePoint = timePoint;
    }

    public void run() {
        Connection con = null;
        PreparedStatement ps = null;

        try {
            LockSupport.parkNanos(timePoint - System.nanoTime());//concurrent timePoint

            con = ds.getConnection();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(9));//delay 9seconds to execute sql
            ps = con.prepareStatement("select * from dummyTable");
            ps.execute();

        } catch (Exception e) {
        } finally {
            if (ps != null) try {
                ps.close();
            } catch (Exception e) {
            }
            if (con != null) try {
                con.close();
            } catch (Exception e) {
            }
        }
        latch.countDown();
    }
}
