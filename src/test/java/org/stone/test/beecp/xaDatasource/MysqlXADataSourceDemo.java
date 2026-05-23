/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.xaDatasource;

import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * CREATE TABLE `account_from` (
 * `id` decimal(10,0) NOT NULL,
 * `money` decimal(10,0) DEFAULT NULL
 * );
 * INSERT INTO test.account_from(id, money)VALUES(1, '10000000000');
 * <p>
 * CREATE TABLE `account_to` (
 * `id` decimal(10,0) NOT NULL,
 * `money` varchar(100) DEFAULT NULL
 * );
 * INSERT INTO test.account_to(id, money)VALUES(1, '50');
 */

public class MysqlXADataSourceDemo {

    public static void main(String[] arg) throws SQLException, XAException {
        BeeDataSourceConfig config1 = new BeeDataSourceConfig();
        config1.setConnectionFactoryClassName("com.mysql.cj.jdbc.MysqlXADataSource");//XaDatasource className
        config1.addConnectionFactoryProperty("user", "root");
        config1.addConnectionFactoryProperty("password", "root");
        config1.addConnectionFactoryProperty("URL", "jdbc:mysql://localhost:3306/test");

        BeeDataSourceConfig config2 = new BeeDataSourceConfig();
        config2.setConnectionFactoryClassName("com.mysql.cj.jdbc.MysqlXADataSource");//XaDatasource className
        config2.addConnectionFactoryProperty("user", "root");
        config2.addConnectionFactoryProperty("password", "root");
        config2.addConnectionFactoryProperty("URL", "jdbc:mysql://localhost:3306/test");

        try (BeeDataSource ds1 = new BeeDataSource(config1);
             BeeDataSource ds2 = new BeeDataSource(config2)) {

            XAConnection xaConnection1 = ds1.getXAConnection();
            XAConnection xaConnection2 = ds2.getXAConnection();

            try (Connection connection1 = xaConnection1.getConnection();
                 Connection connection2 = xaConnection2.getConnection()) {

                XAResource xaResource1 = xaConnection1.getXAResource();
                XAResource xaResource2 = xaConnection2.getXAResource();
                Statement statement1 = connection1.createStatement();
                Statement statement2 = connection2.createStatement();

                Xid xid1 = new MysqlXid(new byte[]{0x01}, new byte[]{0x02}, 100);
                Xid xid2 = new MysqlXid(new byte[]{0x03}, new byte[]{0x04}, 100);

                xaResource1.start(xid1, XAResource.TMNOFLAGS);
                //minus 100
                int update1Result = statement1.executeUpdate("update account_from set money=money-100 where id=1");
                xaResource1.end(xid1, XAResource.TMSUCCESS);

                xaResource2.start(xid2, XAResource.TMNOFLAGS);
                //add 100
                int update2Result = statement2.executeUpdate("update account_to set money= money + 100 where id=1");
                xaResource2.end(xid2, XAResource.TMSUCCESS);

                int ret1 = xaResource1.prepare(xid1);
                int ret2 = xaResource2.prepare(xid2);

                if (XAResource.XA_OK == ret1 && XAResource.XA_OK == ret2) {
                    xaResource1.commit(xid1, false);
                    xaResource2.commit(xid2, false);

                    System.out.println("rows:" + update1Result + ", result2:" + update2Result);
                }
            }
        }
    }
}
