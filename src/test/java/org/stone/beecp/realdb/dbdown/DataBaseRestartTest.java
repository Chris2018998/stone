package org.stone.beecp.realdb.dbdown;

import org.stone.beecp.BeeDataSource;

public class DataBaseRestartTest {

    public static void test(BeeDataSource ds) {
        //step1:get alive count in pool

        //step2:get alive count in pool when db shutdown(pass condition: alive count ==0)

        //step3: get alive count in pool after db start(pass condition: alive count ==1)
    }
}
