/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.tulip.wall;

/**
 * DB Object access permit definition
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AccessPermit {

    private String objectType;//table,view,function,

    private String objectName;//table name

    private String operation;//select,delete,update,insert

    private boolean operationAllowInd;//
    
    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isOperationAllowInd() {
        return operationAllowInd;
    }

    public void setOperationAllowInd(boolean operationAllowInd) {
        this.operationAllowInd = operationAllowInd;
    }
}
