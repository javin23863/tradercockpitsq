/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

public final class SwingUtils {
    static Set<String> setExclude = new HashSet<String>();

    private SwingUtils() {
        throw new Error("SwingUtils is just a container for static methods");
    }

    public static <T extends JComponent> List<T> getDescendantsOfType(Class<T> clazz, Container container) {
        return SwingUtils.getDescendantsOfType(clazz, container, true);
    }

    public static <T extends JComponent> List<T> getDescendantsOfType(Class<T> clazz, Container container, boolean bl) {
        ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
        for (Component component : container.getComponents()) {
            if (clazz.isAssignableFrom(component.getClass())) {
                arrayList.add((JComponent)clazz.cast(component));
            }
            if (!bl && clazz.isAssignableFrom(component.getClass())) continue;
            arrayList.addAll(SwingUtils.getDescendantsOfType(clazz, (Container)component, bl));
        }
        return arrayList;
    }

    public static <T extends JComponent> T getDescendantOfType(Class<T> clazz, Container container, String string, Object object) throws IllegalArgumentException {
        return SwingUtils.getDescendantOfType(clazz, container, string, object, true);
    }

    public static <T extends JComponent> T getDescendantOfType(Class<T> clazz, Container container, String string, Object object, boolean bl) throws IllegalArgumentException {
        List<T> list = SwingUtils.getDescendantsOfType(clazz, container, bl);
        return SwingUtils.getComponentFromList(clazz, list, string, object);
    }

    public static <T extends JComponent> List<T> getDescendantsOfClass(Class<T> clazz, Container container) {
        return SwingUtils.getDescendantsOfClass(clazz, container, true);
    }

    public static <T extends JComponent> List<T> getDescendantsOfClass(Class<T> clazz, Container container, boolean bl) {
        ArrayList<JComponent> arrayList = new ArrayList<JComponent>();
        for (Component component : container.getComponents()) {
            if (clazz.equals(component.getClass())) {
                arrayList.add((JComponent)clazz.cast(component));
            }
            if (!bl && clazz.equals(component.getClass())) continue;
            arrayList.addAll(SwingUtils.getDescendantsOfClass(clazz, (Container)component, bl));
        }
        return arrayList;
    }

    public static <T extends JComponent> T getDescendantOfClass(Class<T> clazz, Container container, String string, Object object) throws IllegalArgumentException {
        return SwingUtils.getDescendantOfClass(clazz, container, string, object, true);
    }

    public static <T extends JComponent> T getDescendantOfClass(Class<T> clazz, Container container, String string, Object object, boolean bl) throws IllegalArgumentException {
        List<T> list = SwingUtils.getDescendantsOfClass(clazz, container, bl);
        return SwingUtils.getComponentFromList(clazz, list, string, object);
    }

    private static <T extends JComponent> T getComponentFromList(Class<T> clazz, List<T> list, String string, Object object) throws IllegalArgumentException {
        T t = null;
        Method method = null;
        try {
            method = clazz.getMethod("get" + string, new Class[0]);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            try {
                method = clazz.getMethod("is" + string, new Class[0]);
            }
            catch (NoSuchMethodException noSuchMethodException2) {
                throw new IllegalArgumentException("Property " + string + " not found in class " + clazz.getName());
            }
        }
        try {
            for (JComponent jComponent : list) {
                Object object2 = method.invoke((Object)jComponent, new Object[0]);
                if (!SwingUtils.equals(object, object2)) continue;
                return (T)jComponent;
            }
        }
        catch (InvocationTargetException invocationTargetException) {
            throw new IllegalArgumentException("Error accessing property " + string + " in class " + clazz.getName());
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new IllegalArgumentException("Property " + string + " cannot be accessed in class " + clazz.getName());
        }
        catch (SecurityException securityException) {
            throw new IllegalArgumentException("Property " + string + " cannot be accessed in class " + clazz.getName());
        }
        return t;
    }

    public static boolean equals(Object object, Object object2) {
        return object == null ? object2 == null : object.equals(object2);
    }

    public static Map<JComponent, List<JComponent>> getComponentMap(JComponent jComponent, boolean bl) {
        HashMap<JComponent, List<JComponent>> hashMap = new HashMap<JComponent, List<JComponent>>();
        for (JComponent jComponent2 : SwingUtils.getDescendantsOfType(JComponent.class, jComponent, false)) {
            if (!hashMap.containsKey(jComponent)) {
                hashMap.put(jComponent, new ArrayList());
            }
            hashMap.get(jComponent).add(jComponent2);
            if (!bl) continue;
            hashMap.putAll(SwingUtils.getComponentMap(jComponent2, bl));
        }
        return hashMap;
    }

    public static UIDefaults getUIDefaultsOfClass(Class clazz) {
        String string = clazz.getName();
        string = string.substring(string.lastIndexOf(".") + 2);
        return SwingUtils.getUIDefaultsOfClass(string);
    }

    public static UIDefaults getUIDefaultsOfClass(String string) {
        UIDefaults uIDefaults = new UIDefaults();
        UIDefaults uIDefaults2 = UIManager.getLookAndFeelDefaults();
        ArrayList arrayList = Collections.list(uIDefaults2.keys());
        for (Object e : arrayList) {
            String string2;
            if (!(e instanceof String) || !((String)e).startsWith(string)) continue;
            String string3 = string2 = (String)e;
            if (string2.contains(".")) {
                string3 = string2.substring(string2.indexOf(".") + 1);
            }
            uIDefaults.put(string3, uIDefaults2.get(e));
        }
        return uIDefaults;
    }

    public static Object getUIDefaultOfClass(Class clazz, String string) {
        Object object = null;
        UIDefaults uIDefaults = SwingUtils.getUIDefaultsOfClass(clazz);
        ArrayList arrayList = Collections.list(uIDefaults.keys());
        for (Object e : arrayList) {
            if (e.equals(string)) {
                return uIDefaults.get(e);
            }
            if (!e.toString().equalsIgnoreCase(string)) continue;
            object = uIDefaults.get(e);
        }
        return object;
    }

    public static Map<Object, Object> getProperties(JComponent jComponent) {
        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();
        Class<?> clazz = jComponent.getClass();
        Method[] methodArray = clazz.getMethods();
        Object object = null;
        for (Method method : methodArray) {
            if (!method.getName().matches("^(is|get).*") || method.getParameterTypes().length != 0) continue;
            try {
                Class<?> clazz2 = method.getReturnType();
                if (clazz2 == Void.TYPE || clazz2.getName().startsWith("[") || setExclude.contains(method.getName())) continue;
                String string = method.getName();
                object = method.invoke((Object)jComponent, new Object[0]);
                if (object == null || object instanceof Component) continue;
                hashMap.put(string, object);
            }
            catch (IllegalAccessException illegalAccessException) {
            }
            catch (IllegalArgumentException illegalArgumentException) {
            }
            catch (InvocationTargetException invocationTargetException) {
                // empty catch block
            }
        }
        return hashMap;
    }

    public static <T extends JComponent> Class getJClass(T t) {
        Class<?> clazz = t.getClass();
        while (!clazz.getName().matches("javax.swing.J[^.]*$")) {
            clazz = clazz.getSuperclass();
        }
        return clazz;
    }

    static {
        setExclude.add("getFocusCycleRootAncestor");
        setExclude.add("getAccessibleContext");
        setExclude.add("getColorModel");
        setExclude.add("getGraphics");
        setExclude.add("getGraphicsConfiguration");
    }
}

