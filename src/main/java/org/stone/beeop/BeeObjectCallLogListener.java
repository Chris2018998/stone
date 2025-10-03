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
 * A listener interface need be implemented by subClass
 *
 * @author Chris Liao
 */
public class BeeObjectCallLogListener<K, V> {

    /**
     * Process slow logs and exception logs in sync mode
     *
     * @param log is a slow log or an exception log
     */
    public void process(BeeObjectCallLog<K, V> log) {
        //empty body
    }

    /**
     * Process a log list which contains some slow logs and exception logs in async mode.
     *
     * @param logList is a log list need be process
     */
    public void process(List<BeeObjectCallLog<K, V>> logList) {
        //empty body
    }

}
