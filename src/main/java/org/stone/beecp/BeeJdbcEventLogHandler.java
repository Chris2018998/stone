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
package org.stone.beecp;

import java.util.List;

/**
 * Log handler interface, {@link BeeJdbcEventLogManager} drives its implementation instance to handle slow logs and exception logs.
 *
 * @author Chris Liao
 */
public interface BeeJdbcEventLogHandler {

    /**
     * Handle slow logs and exception logs in sync mode.
     *
     * @param log is a slow log or an exception log
     * @return true if success to be handled,false that not be handled.
     */
    boolean handle(BeeJdbcEventLog log);

    /**
     * Handle a log list(slow logs and exception logs) in async mode.
     *
     * @param logList contains slow logs and exceptions,may be one type of them
     * @return boolean array of logs handled flag
     */
    boolean[] handle(List<BeeJdbcEventLog> logList);
}
