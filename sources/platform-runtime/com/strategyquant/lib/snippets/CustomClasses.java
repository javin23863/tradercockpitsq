/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.lib.snippets.NonexistingCustomClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClasses<T>
implements ICustomClasses {
    public static final Logger Log = LoggerFactory.getLogger(CustomClasses.class);
    private static final boolean EMIT_ERRORS_TO_LOGGER = true;
    private final HashMap<String, T> availableClassesMap = new HashMap();
    private final ArrayList<T> availableClassesList = new ArrayList();
    private final HashMap<String, String> errorList = new HashMap();
    protected String dirName = null;
    protected Class<T> expectedClassType;

    public void setDirName(String string) {
        if (string == null) {
            throw new IllegalArgumentException("Directory name is null");
        }
        this.dirName = string;
    }

    public void setExpectedClassType(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Expected class type is null");
        }
        this.expectedClassType = clazz;
    }

    public void clearErrorList() {
        this.errorList.clear();
    }

    public Map<String, String> getErrorList() {
        return this.errorList;
    }

    public boolean hasErrors() {
        return !this.errorList.isEmpty();
    }

    public void addError(String string, String string2) {
        if (string != null) {
            this.errorList.put(string, string2);
        } else {
            this.errorList.put("<generic>", string2);
        }
        Log.error(string2);
    }

    public List<T> getAvailableClasses() {
        return this.availableClassesList;
    }

    protected void add(T t) {
        if (t == null) {
            throw new IllegalArgumentException("Instance is null");
        }
        this.availableClassesMap.put(t.getClass().getSimpleName(), t);
        this.availableClassesList.add(t);
    }

    public CustomClasses() {
        CustomClassesReg.add(this);
    }

    public void loadAvailableClasses() {
        this.clearErrorList();
        this.availableClassesMap.clear();
        this.availableClassesList.clear();
        if (this.dirName == null) {
            String string = "Directory not set";
            this.addError(null, string);
            throw new IllegalStateException(string);
        }
        if (this.expectedClassType == null) {
            String string = "Expected class type not set";
            this.addError(null, string);
            throw new IllegalStateException(string);
        }
        try {
            CustomClassesLoader customClassesLoader = new CustomClassesLoader(this.dirName);
            while (customClassesLoader.hasNext()) {
                String string = customClassesLoader.getNext();
                MainApp.drawSplashProgress(String.format("Loading custom class: '%s'", string));
                try {
                    Object object = customClassesLoader.createInstance(string);
                    if (this.expectedClassType.isInstance(object)) {
                        this.add(this.expectedClassType.cast(object));
                        continue;
                    }
                    this.addError(string, String.format("Instantiated class '%s' is not expected type '%s'", string, this.expectedClassType));
                }
                catch (Exception exception) {
                    this.addError(string, String.format("Error loading custom class '%s'. %s", string, exception));
                }
            }
        }
        catch (Exception exception) {
            String string = String.format("Generic error loading custom classes. %s", exception);
            this.addError(null, string);
        }
    }

    public boolean checkClassExists(String string) {
        return this.availableClassesMap.containsKey(string);
    }

    public T findClassByName(String string) throws NonexistingCustomClassException {
        if (!this.checkClassExists(string)) {
            throw new NonexistingCustomClassException(string);
        }
        return this.availableClassesMap.get(string);
    }

    public T createNew(String string, String string2) {
        if (string2 == null) {
            String string3 = "Class name not set";
            this.addError(null, string3);
            throw new IllegalStateException(string3);
        }
        if (string == null) {
            String string4 = "Directory not set";
            this.addError(string2, string4);
            throw new IllegalStateException(string4);
        }
        if (this.expectedClassType == null) {
            String string5 = "Expected class type not set";
            this.addError(string2, string5);
            throw new IllegalStateException(string5);
        }
        CustomClassesLoader customClassesLoader = new CustomClassesLoader(string);
        Object object = customClassesLoader.createInstance(string2);
        if (object == null) {
            return null;
        }
        if (this.expectedClassType.isInstance(object)) {
            return this.expectedClassType.cast(object);
        }
        this.addError(string2, String.format("Instantiated class '%s' is not expected type '%s'", object, this.expectedClassType));
        throw new InstantiationError(String.format("Class not expected type '%s'", this.expectedClassType));
    }

    public T createNew(String string) {
        return this.createNew(this.dirName, string);
    }

    public List<T> cloneAvailableClasses() {
        this.clearErrorList();
        ArrayList<T> arrayList = new ArrayList<T>();
        for (String string : this.availableClassesMap.keySet()) {
            try {
                T t = this.createNew(string);
                arrayList.add(t);
            }
            catch (Error | Exception throwable) {
                this.addError(string, throwable.toString());
            }
        }
        return arrayList;
    }

    @Override
    public void reload() {
        this.loadAvailableClasses();
    }
}

