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

import org.stone.beecp.BeeMethodLog;

import java.util.List;

/**
 * A method log listener.
 *
 * @author Chris Liao
 */
public class MockJdbcMethodLogHandler extends DefaultMethodLogHandler {

    private BeeMethodLog slowLog;

    private BeeMethodLog exceptionLog;

    public BeeMethodLog getSlowLog() {
        return slowLog;
    }

    public BeeMethodLog getExceptionLog() {
        return exceptionLog;
    }

    public void handleStartLog(BeeMethodLog log) {

    }

    public void handleEndLog(BeeMethodLog log) {

    }

    public void handleLongRunningLogs(List<BeeMethodLog> slowList) {

    }
}
