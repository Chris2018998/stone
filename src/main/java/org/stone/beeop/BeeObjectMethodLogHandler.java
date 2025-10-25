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
package org.stone.beeop;

import java.util.List;

/**
 * A handler interface need be implemented by subClass
 *
 * @author Chris Liao
 */
public interface BeeObjectMethodLogHandler<K, V> {

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     */
    void handleStartLog(BeeObjectMethodLog<K, V> log) throws Exception;

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     */
    void handleEndLog(BeeObjectMethodLog<K, V> log) throws Exception;

    /**
     * Handle a list of long-running logs
     *
     * @param longRunningList to be handled
     */
    void handleLongRunningLogs(List<BeeObjectMethodLog<K, V>> longRunningList);

}
