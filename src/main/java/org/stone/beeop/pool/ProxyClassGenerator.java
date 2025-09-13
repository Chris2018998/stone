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
import java.util.List;

/**
 * Generate proxy class for pooled object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ProxyClassGenerator {

    /**
     * Generate proxy class for pooled object.
     *
     * @param interfaces implemented by object class
     * @return proxy class for
     * @throws Exception when fail to generate proxy class
     */
    public static Class<?> genProxyClassWithInterface(Class[] interfaces) throws Exception {
        ClassPool classPool = ClassPool.getDefault();
        classPool.importPackage("org.stone.beeop");
        classPool.importPackage("org.stone.beeop.pool");
        classPool.importPackage("org.stone.tools");
        classPool.appendClassPath(new LoaderClassPath(ProxyClassGenerator.class.getClassLoader()));

        //1: get Ct classes from interfaces array
        CtClass ctSuperClass = null;
        CtClass[] ctInterfaces = null;
        List<CtClass> interfaceList = new ArrayList<>(interfaces.length);
        for (Class<?> clazz : interfaces) {
            CtClass ctClass = classPool.getCtClass(clazz.getName());
            if (clazz.isInterface()) {
                interfaceList.add(ctClass);
            } else {
                ctSuperClass = ctClass;
            }
        }

        //2: retrieve all methods to be overridden
        List<CtMethod> methodList = new ArrayList<>(1);
        if (ctSuperClass != null) getMethodsFromClass(ctSuperClass, methodList);
        if (!interfaceList.isEmpty()) {
            ctInterfaces = interfaceList.toArray(new CtClass[0]);
            getMethodsFromInterfaces(ctInterfaces, methodList);
        }

        //3: make a proxy class
        String proxyClassName = "org.stone.beeop.pool.ObjectProxy" + System.currentTimeMillis();
        CtClass ctProxyObjectClass = classPool.makeClass(proxyClassName);
        if (ctSuperClass != null) ctProxyObjectClass.setSuperclass(ctSuperClass);
        if (ctInterfaces != null) ctProxyObjectClass.setInterfaces(ctInterfaces);

        //4: add new fields
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

        //5: add constructor
        CtClass[] constructorParamTypes = {ctPooledObjectClass, ctProxyHandleClass, ctBeeObjectPredicateClass};
        CtConstructor ctConstructor = new CtConstructor(constructorParamTypes, ctProxyObjectClass);
        ctConstructor.setBody("{this.p=$1;this.raw=$1.raw;this.handle=$2;this.predicate=$3;}");
        ctProxyObjectClass.addConstructor(ctConstructor);

        //6: add methods to proxy class
        StringBuilder methodBuffer = new StringBuilder(50);
        for (CtMethod ctMethod : methodList) {
            String methodName = ctMethod.getName();
            CtMethod newCtMethod = CtNewMethod.copy(ctMethod, ctProxyObjectClass, null);
            newCtMethod.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
            methodBuffer.delete(0, methodBuffer.length());

            methodBuffer.append("{");
            methodBuffer.append("handle.checkClosed();");

            methodBuffer.append("try{");
            if (ctMethod.getReturnType() == CtClass.voidType) {
                methodBuffer.append("this.raw.").append(methodName).append("($$);");
                methodBuffer.append("p.lastAccessTime=System.currentTimeMillis();");
            } else {
                methodBuffer.append(ctMethod.getReturnType().getName()).append(" r=this.raw.").append(methodName).append("($$);");
                methodBuffer.append("p.lastAccessTime=System.currentTimeMillis();");
                methodBuffer.append("return r;");
            }

            methodBuffer.append("} catch (Exception e) {");
            methodBuffer.append(" if (predicate != null && CommonUtil.isNotBlank(predicate.evictTest(e)))");
            methodBuffer.append("  p.abortSelf(ObjectPoolStatics.DESC_RM_BAD);");
            methodBuffer.append("  throw e;");
            methodBuffer.append("}");

            methodBuffer.append("}");
            newCtMethod.setBody(methodBuffer.toString());
            ctProxyObjectClass.addMethod(newCtMethod);
        }

        //6: get Java class
        return ctProxyObjectClass.toClass();
    }

    private static void getMethodsFromInterfaces(CtClass[] ctInterfaces, List<CtMethod> methodList) throws NotFoundException {
        for (CtClass ctInterface : ctInterfaces) {
            getMethodsFromClass(ctInterface, methodList);
        }
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
}
