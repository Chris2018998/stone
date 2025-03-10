/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@Threads(8)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

/**
 * Connection JMH performance
 *
 * @author Chris Liao
 */

public class ConnectionBench {
    private BeeDataSource ds;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ConnectionBench.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public Connection cycleConnection() throws SQLException {
        Connection connection = ds.getConnection();
        connection.close();
        return connection;
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setDriverClassName("org.stone.beecp.driver.MockDriver");
        config.setJdbcUrl("jdbc:beecp://localhost/testdb");
        config.setUsername("test");
        config.setPassword("test");
        config.setInitialSize(1);
        config.setMaxActive(Runtime.getRuntime().availableProcessors());
        config.setMaxWait(8000);
        config.setDefaultAutoCommit(Boolean.FALSE);
        ds = new BeeDataSource(config);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        ds.close();
    }
}
