
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Method execution listener
 *
 * @author Chris Liao
 */
public abstract class LogListener implements BeeMethodLogListener<String> {
    protected final Map<String, Throwable> methodExceptionMap = new HashMap<>(1);
    private final Logger logger = LoggerFactory.getLogger(LogListener.class);
    protected int targetLogType;
    protected boolean nullOfOnLongRunningDetected;

    public void setTargetLogType(int targetLogType) {
        this.targetLogType = targetLogType;
    }

    public void setNullOfOnLongRunningDetected(boolean nullOfOnLongRunningDetected) {
        this.nullOfOnLongRunningDetected = nullOfOnLongRunningDetected;
    }

    public void removeMethodException(String listenerMethodName) {
        this.methodExceptionMap.remove(listenerMethodName);
    }

    public void addMethodException(String listenerMethodName, Throwable methodException) {
        this.methodExceptionMap.put(listenerMethodName, methodException);
    }

    protected void beforeMethod(String listenerMethodName, BeeMethodLog<String> log) throws Exception {
        if (targetLogType == 0 || log.getType() == targetLogType) {
            logger.info("{}.{}", this.getClass().getSimpleName(), listenerMethodName);
            if (methodExceptionMap.containsKey(listenerMethodName)) {
                Throwable failCause = methodExceptionMap.get(listenerMethodName);
                if (failCause instanceof Exception)
                    throw (Exception) failCause;
                else
                    throw (Error) failCause;
            }
        }
    }

    protected List<Boolean> getLongRunningDetected(List<BeeMethodLog<String>> slowList) {
        logger.info("{}.onLongRunningDetected", this.getClass().getSimpleName());
        Throwable failCause = methodExceptionMap.get("onLongRunningDetected");
        if (failCause instanceof Error) throw (Error) failCause;
        if (nullOfOnLongRunningDetected) return null;

        List<Boolean> booleanList = new ArrayList<>(slowList.size());
        for (int i = 0, size = slowList.size(); i < size; i++) {
            booleanList.add(Boolean.TRUE);
        }
        return booleanList;
    }
}
