/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.classInfo;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.strategyquant.lib.classInfo.ClazzInfo;
import com.strategyquant.lib.classInfo.CodeInfo;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class ClassHelper {
    private static final Class[] java_classes = new Class[]{Math.class, Random.class, Collections.class, Arrays.class, Date.class, Map.class, HashMap.class, Set.class, TreeSet.class, HashSet.class, Byte.class, String.class, Long.class, Integer.class, Boolean.class, Short.class, List.class, ArrayList.class, LinkedList.class};

    public static List<Class> getClasses(String[] stringArray) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = ClassHelper.class.getClassLoader();
        ClassPath classPath = ClassPath.from((ClassLoader)classLoader);
        ImmutableSet immutableSet = classPath.getAllClasses();
        LinkedList<Class> linkedList = new LinkedList<Class>();
        block2: for (ClassPath.ClassInfo classInfo : immutableSet) {
            try {
                for (String string : stringArray) {
                    if (!classInfo.getName().startsWith(string)) continue;
                    linkedList.add(classLoader.loadClass(classInfo.getName()));
                    continue block2;
                }
            }
            catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        for (Class clazz : java_classes) {
            linkedList.add(clazz);
        }
        return linkedList;
    }

    private static ClazzInfo.MethodInfo prepareMethod(String string, List<ClazzInfo.MethodInfo> list, Map<String, ClazzInfo.MethodInfo> map) {
        ClazzInfo.MethodInfo methodInfo = map.get(string);
        if (methodInfo == null) {
            methodInfo = new ClazzInfo.MethodInfo(string);
            map.put(string, methodInfo);
            list.add(methodInfo);
        }
        return methodInfo;
    }

    public static ClazzInfo getClazzInfo(Class clazz) {
        Method[] methodArray;
        String string = clazz.getSimpleName();
        if (string == null || string.trim().isEmpty()) {
            return null;
        }
        ClazzInfo clazzInfo = new ClazzInfo();
        clazzInfo.setFullName(clazz.getCanonicalName());
        clazzInfo.setName(string);
        LinkedList<ClazzInfo.ParamInfo> linkedList = new LinkedList<ClazzInfo.ParamInfo>();
        clazzInfo.setFields(linkedList);
        LinkedList<ClazzInfo.ParamInfo> linkedList2 = new LinkedList<ClazzInfo.ParamInfo>();
        clazzInfo.setStaticFields(linkedList2);
        for (Field field : clazz.getFields()) {
            if (!Modifier.isPublic(field.getModifiers())) continue;
            try {
                methodArray = new ClazzInfo.ParamInfo();
                methodArray.setName(field.getName());
                methodArray.setType(field.getType().getSimpleName());
                boolean bl = Modifier.isStatic(field.getModifiers());
                if (bl) {
                    linkedList2.add((ClazzInfo.ParamInfo)methodArray);
                    continue;
                }
                linkedList.add((ClazzInfo.ParamInfo)methodArray);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        LinkedList<ClazzInfo.MethodInfo> linkedList3 = new LinkedList<ClazzInfo.MethodInfo>();
        clazzInfo.setMethods(linkedList3);
        LinkedList<ClazzInfo.MethodInfo> linkedList4 = new LinkedList<ClazzInfo.MethodInfo>();
        clazzInfo.setStaticMethods(linkedList4);
        HashMap<String, ClazzInfo.MethodInfo> hashMap = new HashMap<String, ClazzInfo.MethodInfo>();
        HashMap<String, ClazzInfo.MethodInfo> hashMap2 = new HashMap<String, ClazzInfo.MethodInfo>();
        for (Method method : methodArray = clazz.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            try {
                boolean bl = Modifier.isStatic(method.getModifiers());
                ClazzInfo.MethodInfo methodInfo = null;
                methodInfo = bl ? ClassHelper.prepareMethod(method.getName(), linkedList4, hashMap) : ClassHelper.prepareMethod(method.getName(), linkedList3, hashMap2);
                methodInfo.setResultType(method.getReturnType().getSimpleName());
                LinkedList<ClazzInfo.ParamInfo> linkedList5 = new LinkedList<ClazzInfo.ParamInfo>();
                methodInfo.getParams().add(linkedList5);
                for (Parameter parameter : method.getParameters()) {
                    ClazzInfo.ParamInfo paramInfo = new ClazzInfo.ParamInfo();
                    paramInfo.setName(parameter.getName());
                    paramInfo.setType(parameter.getType().getSimpleName());
                    linkedList5.add(paramInfo);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return clazzInfo;
    }

    public static CodeInfo getInfos(String[] stringArray) throws ClassNotFoundException, IOException {
        List<Class> list = ClassHelper.getClasses(stringArray);
        CodeInfo codeInfo = new CodeInfo();
        HashSet<String> hashSet = new HashSet<String>();
        codeInfo.setImports(hashSet);
        LinkedList<ClazzInfo> linkedList = new LinkedList<ClazzInfo>();
        codeInfo.setClasses(linkedList);
        for (Class clazz : list) {
            try {
                ClazzInfo clazzInfo = ClassHelper.getClazzInfo(clazz);
                if (clazzInfo == null) continue;
                hashSet.add(clazz.getPackage().getName());
                linkedList.add(clazzInfo);
            }
            catch (Throwable throwable) {}
        }
        return codeInfo;
    }

    public static void main(String[] stringArray) throws IOException, ClassNotFoundException {
        ClassHelper.getInfos(new String[0]);
    }

    public static String getClassesJson(String[] stringArray) throws ClassNotFoundException, IOException {
        CodeInfo codeInfo = ClassHelper.getInfos(stringArray);
        return codeInfo.toJSON();
    }
}

