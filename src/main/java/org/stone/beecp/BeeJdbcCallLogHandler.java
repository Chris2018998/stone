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
 * A handler interface,its implementation class is driven by {@link BeeJdbcCallLogManager} to handle slow logs and exception logs.
 *
 * @author Chris Liao
 */
public interface BeeJdbcCallLogHandler {

    /**
     * Handle slow logs and exception logs in sync mode.
     *
     * @param log is a slow log or an exception log
     * @return boolean,true is that log is handled
     */
    boolean handle(BeeJdbcCallLog log);

    /**
     * Handle a log list(slow logs and exception logs) in async mode.
     *
     * @param logList is a log list need be handle
     * @return boolean array,length must equal the size of log list
     */
    boolean[] handle(List<BeeJdbcCallLog> logList);
}
