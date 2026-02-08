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
package org.stone.test.beeop.objects.methodLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.BeeMethodLog;
import org.stone.beeop.BeeMethodLogListener;

import java.util.List;

/**
 * Method execution listener
 *
 * @author Chris Liao
 */
public class LogListener1 implements BeeMethodLogListener<String> {
    private final Logger logger = LoggerFactory.getLogger(LogListener1.class);

    private BeeMethodLog<String> slowLog;

    private BeeMethodLog<String> exceptionLog;

    public BeeMethodLog<String> getSlowLog() {
        return slowLog;
    }

    public BeeMethodLog<String> getExceptionLog() {
        return exceptionLog;
    }

    public void onMethodStart(BeeMethodLog<String> log) {
        logger.info("onMethodStart");
    }

    public void onMethodEnd(BeeMethodLog<String> log) {
        logger.info("onMethodEnd");
        if (log.isException()) {
            exceptionLog = log;
        } else if (log.isSlow()) {
            this.slowLog = log;
        }
    }

    public List<Boolean> onLongRunningDetected(List<BeeMethodLog<String>> slowList) {
        return null;
    }
}
