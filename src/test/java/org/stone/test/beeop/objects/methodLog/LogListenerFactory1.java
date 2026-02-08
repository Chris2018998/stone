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

import org.stone.beeop.BeeMethodLogListener;
import org.stone.beeop.BeeMethodLogListenerFactory;
import org.stone.beeop.BeeObjectSourceConfig;

/**
 * Method execution listener factory, for success test
 *
 * @author Chris Liao
 */
public class LogListenerFactory1 implements BeeMethodLogListenerFactory<String> {

    /**
     * Creates method execution listener.
     *
     * @return created Listener instance
     */
    public BeeMethodLogListener<String> create(BeeObjectSourceConfig<String, ?> config) throws Exception {
        return new LogListener1();
    }
}
