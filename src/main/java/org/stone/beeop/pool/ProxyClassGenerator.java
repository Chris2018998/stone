/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import javassist.*;
import org.stone.beeop.BeeObjectPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generate proxy class for pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ProxyClassGenerator {

    /**
     * Generate two proxy classes for pooled object
     *
     * @param interfaces implemented by object class
     * @return two generated proxy classes(first proxy as base wrapper,second proxy to support method logs cache)
     * @throws Exception when fail to generate proxy class
     */
    public static Class<?>[] genProxyClassWithInterface(Class<?> superClass, Class[] interfaces, List<String> listenMethodNameList) throws Exception {
        //1: import some class packages
        ClassPool classPool = new ClassPool(true);
        classPool.importPackage("org.stone.beeop");
        classPool.importPackage("org.stone.beeop.pool");
        classPool.importPackage("org.stone.tools");
        classPool.appendClassPath(new LoaderClassPath(ProxyClassGenerator.class.getClassLoader()));

        //2: Create a list to collect methods to be overridden in proxy class
        List<CtMethod> methodList = new ArrayList<>(16);

        //3: load CtClass of super class
        CtClass ctSuperClass = null;
        if (superClass != null) {
            ctSuperClass = classPool.getCtClass(superClass.getName());
            getMethodsFromClass(ctSuperClass, methodList);
        }

        //4: load CtClasses of interfaces
        CtClass[] ctInterfaces = null;
        if (interfaces != null) {
            ArrayList<CtClass> ctInterfaceList = new ArrayList<>(interfaces.length);
            for (Class<?> clazz : interfaces) {
                CtClass ctInterface = classPool.getCtClass(clazz.getName());
                ctInterfaceList.add(ctInterface);
                getMethodsFromClass(ctInterface, methodList);
            }
            ctInterfaces = ctInterfaceList.toArray(new CtClass[0]);
        }

        //5: Generate a proxy class
        Class<?> proxyClass1 = genProxyClass(classPool, ctSuperClass, ctInterfaces, methodList, false, null);

        //6: Generate a proxy class to support method execution logs
        Class<?> proxyClass2 = genProxyClass(classPool, ctSuperClass, ctInterfaces, methodList, true, listenMethodNameList);

        //7: return an array of generated proxy classes
        return new Class[]{proxyClass1, proxyClass2};
    }

    //Generate an object proxy class without method execution logs cache
    private static Class<?> genProxyClass(ClassPool classPool, CtClass ctSuperClass, CtClass[] ctInterfaces, List<CtMethod> methodList, boolean collectMethodLogs, List<String> listenMethodNameList) throws Exception {
        //Class1: org.stone.beeop.pool.ObjectProxy_currentTimeMillis.class
        // public ObjectProxy_currentTimeMillis(PooledObject p,PooledObjectProxyHandle handle,BeeObjectPredicate predicate){
        //  ......
        // }

        //Class2:org.stone.beeop.pool.ObjectProxy_4L_currentTimeMillis.class
        // public ObjectProxy_4L_currentTimeMillis(PooledObject p,PooledObjectProxyHandle handle,BeeObjectPredicate predicate,Object key,MethodExecutionLogCache cache){
        //  ......
        // }

        //1: make a proxy class with milliseconds
        String proxyClassName = collectMethodLogs ? "org.stone.beeop.pool.ObjectProxy_4L_" + System.currentTimeMillis() : "org.stone.beeop.pool.ObjectProxy_" + System.currentTimeMillis();
        CtClass ctProxyObjectClass = classPool.makeClass(proxyClassName);
        ctProxyObjectClass.setGenericSignature("K");
        if (ctSuperClass != null) ctProxyObjectClass.setSuperclass(ctSuperClass);
        if (ctInterfaces != null) ctProxyObjectClass.setInterfaces(ctInterfaces);

        //2: add four fields to the new proxy class
        //field1(private final proxyClassName raw;)
        CtField ctRawField = new CtField(ctProxyObjectClass, "raw", ctProxyObjectClass);
        ctRawField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        ctProxyObjectClass.addField(ctRawField);
        //field2(private final PooledObject p;)
        CtClass ctPooledObjectClass = classPool.getCtClass(PooledObject.class.getName());
        CtField ctPooledObjectField = new CtField(ctPooledObjectClass, "p", ctProxyObjectClass);
        ctPooledObjectField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        ctProxyObjectClass.addField(ctPooledObjectField);
        //field3(private final PooledObjectProxyHandle handle;)
        CtClass ctProxyHandleClass = classPool.getCtClass(PooledObjectProxyHandle.class.getName());
        CtField ctHandleField = new CtField(ctProxyHandleClass, "handle", ctProxyObjectClass);
        ctHandleField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        ctProxyObjectClass.addField(ctHandleField);
        //field4(private final BeeObjectPredicate predicate;)
        CtClass ctBeeObjectPredicateClass = classPool.getCtClass(BeeObjectPredicate.class.getName());
        CtField ctPredicateField = new CtField(ctBeeObjectPredicateClass, "predicate", ctProxyObjectClass);
        ctPredicateField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
        ctProxyObjectClass.addField(ctPredicateField);

        //3: add constructor to the proxy class
        listenMethodNameList = listenMethodNameList == null ? Collections.emptyList() : listenMethodNameList;
        if (!collectMethodLogs) {
            //add constructor to proxy class
            CtClass[] constructorParamTypes = {ctPooledObjectClass, ctProxyHandleClass, ctBeeObjectPredicateClass};
            CtConstructor ctConstructor = new CtConstructor(constructorParamTypes, ctProxyObjectClass);
            ctConstructor.setBody("{this.p=$1;this.raw=$1.raw;this.handle=$2;this.predicate=$3;}");
            ctProxyObjectClass.addConstructor(ctConstructor);
        } else {
            //field5(private final Object key;)
            CtClass ctKeyClass = classPool.get("java.lang.Object");
            CtField ctKeyField = new CtField(ctKeyClass, "key", ctProxyObjectClass);
            ctKeyField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            ctProxyObjectClass.addField(ctKeyField);

            //field6(private final MethodExecutionLogCache<K> logCache;)
            CtClass ctMethodExecutionLogCacheClass = classPool.getCtClass(MethodExecutionLogCache.class.getName());
            CtField ctLogCacheField = new CtField(ctMethodExecutionLogCacheClass, "logCache", ctProxyObjectClass);
            ctLogCacheField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            ctProxyObjectClass.addField(ctLogCacheField);

            //add constructor to proxy class
            CtClass[] constructorParamTypes = {ctPooledObjectClass, ctProxyHandleClass, ctBeeObjectPredicateClass, ctKeyClass, ctMethodExecutionLogCacheClass};
            CtConstructor ctConstructor = new CtConstructor(constructorParamTypes, ctProxyObjectClass);
            ctConstructor.setBody("{this.p=$1;this.raw=$1.raw;this.handle=$2;this.predicate=$3;this.key=$4;this.logCache=$5;}");
            ctProxyObjectClass.addConstructor(ctConstructor);
        }

        //4: add methods to proxy class
        StringBuilder methodBuffer = new StringBuilder(100);
        for (CtMethod ctMethod : methodList) {
            String methodName = ctMethod.getName();
            CtMethod newCtMethod = CtNewMethod.copy(ctMethod, ctProxyObjectClass, null);
            newCtMethod.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
            methodBuffer.delete(0, methodBuffer.length());

            //if exists method exceptions,then add 'try{......}catch(Exception1|Exception2|Exception3 e){}' to wrap method call
            String exceptionSnippetsCodes = constructExceptionsForCatch(ctMethod.getExceptionTypes());
            boolean needAddTry = exceptionSnippetsCodes != null && !exceptionSnippetsCodes.isEmpty();

            //Method body start
            methodBuffer.append("{");
            methodBuffer.append("handle.checkClosed();");//check handle is whether closed

            if (collectMethodLogs && listenMethodNameList.contains(methodName)) {//need collect method execution log
                String methodSignature = getCtMethodSignature("Object", ctMethod);
                CtClass[] parameterTypes = ctMethod.getParameterTypes();
                int methodParameterSize = parameterTypes.length;
                if (methodParameterSize == 0) {
                    methodBuffer.append("BeeMethodExecutionLog log = logCache.beforeCall(key,BeeMethodExecutionLog.Type_Object_Call,").append(methodSignature).append(",null);");
                } else {
                    methodBuffer.append("Object[]parameters = new Object[]{");
                    for (int i = 0; i < methodParameterSize; i++) {
                        if (i > 0) methodBuffer.append(",");
                        methodBuffer.append(getConvertType("$" + (i + 1), parameterTypes[i]));
                    }
                    methodBuffer.append("};");
                    methodBuffer.append("BeeMethodExecutionLog log = logCache.beforeCall(key,BeeMethodExecutionLog.Type_Object_Call,").append(methodSignature).append(",parameters);");
                }

                //catch
                if (needAddTry) methodBuffer.append("try{");
                CtClass ctReturnType = ctMethod.getReturnType();
                //add method call on pooled object
                if (ctReturnType == CtClass.voidType) {
                    methodBuffer.append("this.raw.").append(methodName).append("($$);");
                    methodBuffer.append("p.lastAccessTime=System.currentTimeMillis();");
                    methodBuffer.append("logCache.afterCall(null,log);");
                } else {
                    methodBuffer.append(ctReturnType.getName()).append(" r=this.raw.").append(methodName).append("($$);");
                    methodBuffer.append("p.lastAccessTime=System.currentTimeMillis();");
                    methodBuffer.append("logCache.afterCall(").append(getConvertType("r", ctReturnType)).append(",log);");
                    methodBuffer.append("return r;");
                }

                //append exception catch to method
                if (needAddTry) {
                    methodBuffer.append("} catch (").append(exceptionSnippetsCodes).append(" e) {");
                    methodBuffer.append(" if (predicate != null && CommonUtil.isNotBlank(predicate.evictionTest(e)))");
                    methodBuffer.append("  p.abortSelf(ObjectPoolStatics.DESC_RM_BAD);");
                    methodBuffer.append("  logCache.afterCall(e,log);");
                    methodBuffer.append("  throw e;");
                    methodBuffer.append("}");
                }
            } else {
                //catch
                if (needAddTry) methodBuffer.append("try{");

                //add method call on pooled object
                if (ctMethod.getReturnType() == CtClass.voidType) {
                    methodBuffer.append("this.raw.").append(methodName).append("($$);");
                    methodBuffer.append("p.lastAccessTime=System.currentTimeMillis();");
                } else {
                    methodBuffer.append(ctMethod.getReturnType().getName()).append(" r=this.raw.").append(methodName).append("($$);");
                    methodBuffer.append("p.lastAccessTime=System.currentTimeMillis();");
                    methodBuffer.append("return r;");
                }

                //append exception catch to method
                if (needAddTry) {
                    methodBuffer.append("} catch (").append(exceptionSnippetsCodes).append(" e) {");
                    methodBuffer.append(" if (predicate != null && CommonUtil.isNotBlank(predicate.evictionTest(e)))");
                    methodBuffer.append("  p.abortSelf(ObjectPoolStatics.DESC_RM_BAD);");
                    methodBuffer.append("  throw e;");
                    methodBuffer.append("}");
                }
            }

            methodBuffer.append("}");
            newCtMethod.setBody(methodBuffer.toString());
            ctProxyObjectClass.addMethod(newCtMethod);
        }

        //5: get Java class from the ctClass
        return ctProxyObjectClass.toClass();
    }

    //look up all overridable methods from super class
    private static void getMethodsFromClass(CtClass superClass, List<CtMethod> methodList) throws NotFoundException {
        for (CtMethod ctMethod : superClass.getDeclaredMethods()) {
            int modifiers = ctMethod.getModifiers();
            if (Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers) || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers))
                continue;
            if (!methodList.contains(ctMethod)) {
                methodList.add(ctMethod);
            }
        }

        CtClass parentClass = superClass.getSuperclass();
        if (!parentClass.getName().equals("java.lang.Object")) {
            getMethodsFromClass(parentClass, methodList);
        }
    }

    private static String constructExceptionsForCatch(CtClass[] exceptionTypes) {
        if (exceptionTypes == null || exceptionTypes.length == 0) return null;
        StringBuilder exceptionBuilder = new StringBuilder(100);
        for (CtClass exceptionClass : exceptionTypes) {
            if (!exceptionBuilder.isEmpty()) exceptionBuilder.append("|");
            exceptionBuilder.append(exceptionClass.getName());
        }
        return exceptionBuilder.toString();
    }

    private static String getCtMethodSignature(String methodOwer, CtMethod method) throws Exception {
        StringBuilder builder = new StringBuilder(20);
        builder.append("\"").append(methodOwer).append(".").append(method.getName()).append("(");
        CtClass[] paramTypes = method.getParameterTypes();
        for (int i = 0, l = paramTypes.length; i < l; i++) {
            if (i > 0) builder.append(",");
            builder.append(paramTypes[i].getName());
        }
        builder.append(")").append("\"");
        return builder.toString();
    }

    private static String getConvertType(String variableName, CtClass parameterType) {
        if (parameterType.isPrimitive()) {
            String typeName = parameterType.getName();
            if ("boolean".equals(typeName)) {
                return "Boolean.valueOf(" + variableName + ")";
            } else if ("byte".equals(typeName)) {
                return "Byte.valueOf(" + variableName + ")";
            } else if ("short".equals(typeName)) {
                return "Short.valueOf(" + variableName + ")";
            } else if ("int".equals(typeName)) {
                return "Integer.valueOf(" + variableName + ")";
            } else if ("long".equals(typeName)) {
                return "Long.valueOf(" + variableName + ")";
            } else if ("float".equals(typeName)) {
                return "Float.valueOf(" + variableName + ")";
            } else if ("double".equals(typeName)) {
                return "Double.valueOf(" + variableName + ")";
            } else if ("char".equals(typeName)) {
                return "Character.valueOf(" + variableName + ")";
            } else {
                return variableName;
            }
        } else {
            return variableName;
        }
    }
}
