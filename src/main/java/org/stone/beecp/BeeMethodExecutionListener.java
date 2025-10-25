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

import java.sql.SQLException;
import java.util.List;

/**
 * Method execution listener interface.
 *
 * @author Chris Liao
 */
public interface BeeMethodExecutionListener {

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     */
    void onMethodStart(BeeMethodExecutionLog log) throws SQLException;

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     */
    void onMethodEnd(BeeMethodExecutionLog log) throws SQLException;

    /**
     * Handle a list of long-running logs
     *
     * @param logList to be handled
     */
    void onLongRunningDetected(List<BeeMethodExecutionLog> logList);

}
