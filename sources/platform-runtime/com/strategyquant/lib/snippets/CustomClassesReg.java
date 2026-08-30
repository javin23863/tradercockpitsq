/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.CustomClassesLoaderCache;
import com.strategyquant.lib.snippets.ICustomClasses;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClassesReg {
    public static final Logger Log = LoggerFactory.getLogger(CustomClassesReg.class);
    private static final ArrayList<ICustomClasses> register = new ArrayList();

    private CustomClassesReg() {
    }

    public static void add(ICustomClasses iCustomClasses) {
        register.add(iCustomClasses);
    }

    public static void reloadAll() {
        MainApp.snippetsClassLoader = null;
        CustomClassesLoaderCache.getInstance().reload();
        for (ICustomClasses iCustomClasses : register) {
            try {
                Log.debug("Reloading snippets " + iCustomClasses.getClass().getSimpleName());
                iCustomClasses.reload();
            }
            catch (Error | Exception throwable) {
                Log.error("Error while reloading snippets " + iCustomClasses.getClass().getSimpleName(), throwable);
            }
        }
        try {
            CustomClassesReg.reloadRules();
        }
        catch (Exception exception) {
            Log.error("!!! Exception reloading rules", (Throwable)exception);
        }
        MainApp.reloadApp();
    }

    private static void reloadRules() throws Exception {
        URLClassLoader uRLClassLoader = MainApp.getSnippetsClassLoader();
        Class<?> clazz = uRLClassLoader.loadClass("SQ.Internal.Rules");
        Method method = clazz.getMethod("reload", new Class[0]);
        method.invoke(null, new Object[0]);
    }
}

