package org.stone.beecp.other;

import org.stone.beecp.BeeDataSource;

import java.sql.Connection;

/**
 * 1 Issue
 * Test case for issue #2170 of HikariCP
 * Issue url: https://github.com/brettwooldridge/HikariCP/issues/2170
 *
 * 2 Test info
 * db-installer:PostgreSQL-10.4-1-win64-bigsql.exe
 * jdbc driver: postgresql-42.7.1.jar
 * SQL Execution before test: CREATE SCHEMA schema1;
 *
 * 3 Test Result: passed
 */

public class PostgresSchemaRecoverTest {
//    try (HikariDataSource dataSource = new HikariDataSource()) {
//        // configuring connection settings for Postgres
//        dataSource.setAutoCommit(false);
//        dataSource.setSchema("public");
//        dataSource.setMinimumIdle(1); // to be ensured we take the same connection from pool
//        dataSource.setMaximumPoolSize(1); // to be ensured we take the same connection from pool
//        try (Connection connection = dataSource.getConnection()) {
//            if (!"public".equals(connection.getSchema())) { // OK
//                throw new AssertionError(connection.getSchema());
//            }
//            connection.setSchema("schema1");
//            if (!"schema1".equals(connection.getSchema())) { // OK
//                throw new AssertionError(connection.getSchema());
//            }
//            connection.commit(); // Postgres save default schema in session scope on commit
//            connection.setSchema("public");
//            connection.rollback(); // Postgres restore default schema in session from las commit (schema1) but com.zaxxer.hikari.pool.ProxyConnection::dbschema is still equals to "public" schema
//            if ("public".equals(connection.getSchema())) { // OK ProxyConnection.getSchema is directly calling Postgres connection getSchema()
//                throw new AssertionError(connection.getSchema());
//            }
//        } // while on close there is code in com.zaxxer.hikari.pool.PoolBase :
//            /*
//                // This if statement will be skipped case proxyConnection.getSchemaState() will return not actual schema from variable com.zaxxer.hikari.pool.ProxyConnection::dbschema, so schema in settings is equals to not actual value
//                if ((dirtyBits & 32) != 0 && this.schema != null && !this.schema.equals(proxyConnection.getSchemaState())) {
//                    connection.setSchema(this.schema);
//                    resetBits |= 32;
//                }
//                // proposed solution is to change call to proxyConnection.getSchemaState() to connection.getSchema()
//             */
//        try (Connection connection = dataSource.getConnection()) {
//            if (!"public".equals(connection.getSchema())) { // FAIL cause actual Postgres connection schema is "schema1" (it was not reset on previous ProxyConnection::close)
//                throw new AssertionError(connection.getSchema());
//            }
//        }
//    }

    public static void main(String[]args)throws Exception{
        //step1: set Configuration to bee datasource
        BeeDataSource dataSource = new BeeDataSource();
        dataSource.setDefaultAutoCommit(false);
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(1);
        dataSource.setDefaultSchema("public");
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUsername("postgres");
        dataSource.setPassword("root");

        //two new indicator to support Postgres
        dataSource.setEnableFastDirtyOnSchema(true);
        dataSource.setEnableFastDirtyOnCatalog(true);

        //step2: change Schema
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (!"public".equals(connection.getSchema())) { // OK
                throw new AssertionError(connection.getSchema());
            }
            connection.setSchema("schema1");
            if (!"schema1".equals(connection.getSchema())) { // OK
                throw new AssertionError(connection.getSchema());
            }

            connection.commit(); // Postgres save default schema in session scope on commit
            connection.setSchema("public");
            connection.rollback(); // Postgres restore default schema in session from las commit (schema1) but com.zaxxer.hikari.pool.ProxyConnection::dbschema is still equals to "public" schema
            if ("public".equals(connection.getSchema())) { // OK ProxyConnection.getSchema is directly calling Postgres connection getSchema()
                throw new AssertionError(connection.getSchema());
            }
        }finally {
            if(connection!=null){
                connection.close();//return to pool
            }
        }

        //step3: read schema and compare with default schema
        Connection connection2 = null;
        try {
            connection2 = dataSource.getConnection();
            if (!"public".equals(connection2.getSchema())) { // FAIL cause actual Postgres connection schema is "schema1" (it was not reset on previous ProxyConnection::close)
                throw new AssertionError(connection2.getSchema());
            }
        }finally {
            if(connection2!=null){
                connection2.close();//return to pool
            }
        }
    }
}
