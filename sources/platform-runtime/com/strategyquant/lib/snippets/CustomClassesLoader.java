/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.CustomClassesLoaderCache;
import com.strategyquant.lib.snippets.CustomClassesLoaderException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClassesLoader {
    public static final Logger Log = LoggerFactory.getLogger(CustomClassesLoader.class);
    private Iterator<String> iterator;
    private String packageName;
    private Throwable lastError;
    private URLClassLoader classLoader;

    public CustomClassesLoader(String string) {
        this.clearLastError();
        try {
            this.packageName = ("SQ/" + string).replace("/", ".");
            this.searchForClasses();
        }
        catch (Exception exception) {
            this.setLastError(exception);
            Log.error("Exc.", (Throwable)exception);
        }
    }

    private void searchForClasses() {
        List<String> list = CustomClassesLoaderCache.getInstance().getClasses(this.packageName);
        this.iterator = list.listIterator();
    }

    public Throwable getLastError() {
        return this.lastError;
    }

    private void clearLastError() {
        this.lastError = null;
    }

    private void setLastError(Throwable throwable) {
        this.lastError = throwable;
    }

    public boolean hasNext() {
        this.clearLastError();
        if (this.iterator == null) {
            IllegalStateException illegalStateException = new IllegalStateException("Iterator is not initialized");
            this.setLastError(illegalStateException);
            throw illegalStateException;
        }
        return this.iterator.hasNext();
    }

    public String getNext() {
        this.clearLastError();
        if (this.iterator == null) {
            IllegalStateException illegalStateException = new IllegalStateException("Iterator is not initialized");
            this.setLastError(illegalStateException);
            throw illegalStateException;
        }
        return this.iterator.next();
    }

    public Object createInstanceThrow(String string) throws CustomClassesLoaderException, ClassNotFoundException, InvocationTargetException, InstantiationException, NoSuchMethodException, IllegalAccessException {
        this.clearLastError();
        if (string == null) {
            CustomClassesLoaderException customClassesLoaderException = new CustomClassesLoaderException("Class name is null");
            this.setLastError(customClassesLoaderException);
            throw customClassesLoaderException;
        }
        if (!string.startsWith(this.packageName)) {
            string = this.packageName + "." + string;
        }
        try {
            try {
                this.classLoader = MainApp.getSnippetsClassLoader();
            }
            catch (Exception exception) {
                throw new CustomClassesLoaderException("SnippetsClassLoader - " + exception.getMessage());
            }
            Class<?> clazz = this.classLoader.loadClass(string);
            if (Modifier.isAbstract(clazz.getModifiers())) {
                return null;
            }
            return clazz.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        }
        catch (Error | Exception throwable) {
            if (string.contains("SQ.Columns.Databanks")) {
                Log.error(String.format("Unable to create instance of class '%s'.", string));
            } else {
                Log.error(String.format("Unable to create instance of class '%s'. Exc.", string), throwable);
            }
            this.setLastError(throwable);
            throw throwable;
        }
    }

    public Object createInstance(String string) {
        try {
            return this.createInstanceThrow(string);
        }
        catch (Exception exception) {
            return null;
        }
    }
}

