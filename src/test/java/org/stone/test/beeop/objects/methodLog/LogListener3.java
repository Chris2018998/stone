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

/**
 * Method execution listener
 *
 * @author Chris Liao
 */
public class LogListener3 extends LogListener1 {
    private final Logger logger = LoggerFactory.getLogger(LogListener3.class);

    public void onMethodStart(BeeMethodLog<String> log) {
        logger.info("LogListener3.onMethodStart");
    }

    public void onMethodEnd(BeeMethodLog<String> log) {
        logger.info("LogListener3.onMethodEnd");
    }
}
