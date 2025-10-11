/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.stone.test.beecp.objects.jdbclog;

import org.stone.beecp.BeeJdbcEventLog;
import org.stone.beecp.pool.DefaultJdbcEventLogHandler;

import java.util.List;

/**
 * A method log listener.
 *
 * @author Chris Liao
 */
public class MockJdbcEventLogHandler extends DefaultJdbcEventLogHandler {

    private BeeJdbcEventLog slowLog;

    private BeeJdbcEventLog exceptionLog;

    public BeeJdbcEventLog getSlowLog() {
        return slowLog;
    }

    public BeeJdbcEventLog getExceptionLog() {
        return exceptionLog;
    }

    /**
     * Handle slow logs and exception logs in sync mode
     *
     * @param log is a slow log or an exception log
     */
    public boolean handle(BeeJdbcEventLog log) {
        if (log.getFailCause() == null) {
            this.slowLog = log;
        } else {
            this.exceptionLog = log;
        }
        return super.handle(log);
    }

    /**
     * Handle a log list which contains some slow logs and exception logs in async mode.
     *
     * @param logList is a log list need be handled
     */
    public boolean[] handle(List<BeeJdbcEventLog> logList) {
        return super.handle(logList);
    }
}
