/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.jar;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.db.DbBase;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.jar.CustomJavaFileObject;
import com.strategyquant.lib.time.SQTimeOld;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClassloaderJavaFileManagerJ10
implements JavaFileManager {
    public static final Logger Log = LoggerFactory.getLogger(CustomClassloaderJavaFileManagerJ10.class);
    private static final String[] ALLOWED_PACKAGES = new String[]{"com.strategyquant.lib", "com.strategyquant.lib.constants", "com.strategyquant.lib.memory", "com.strategyquant.lib.snippets", "com.strategyquant.lib.utils"};
    private static final String[] ALLOWED_CLASSES = new String[]{DbBase.class.getName(), SQTimeOld.class.getName(), IXMLAble.class.getName(), MainApp.class.getName()};
    private static final Set<String> ALLOWED_PACKAGES_SET = new HashSet<String>(Arrays.asList(ALLOWED_PACKAGES));
    private static final Set<String> ALLOWED_CLASSES_SET = new HashSet<String>(Arrays.asList(ALLOWED_CLASSES));
    private final ClassLoader classLoader;
    private final StandardJavaFileManager standardFileManager;
    private final Map<String, byte[]> classMap;

    public CustomClassloaderJavaFileManagerJ10(ClassLoader classLoader, StandardJavaFileManager standardJavaFileManager) throws Exception {
        this.classLoader = classLoader;
        this.standardFileManager = standardJavaFileManager;
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("#CMPDL#");
        if (inputStream == null) {
            Log.debug("Unable to retrieve compilation class map");
            this.classMap = this.tryToLoadFromFile();
        } else {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);){
                Object object = objectInputStream.readObject();
                if (object instanceof HashMap) {
                    this.classMap = (HashMap)object;
                } else {
                    Log.error("Incorrect compilation class map type");
                    this.classMap = new HashMap<String, byte[]>();
                }
            }
            try {
                inputStream.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        Log.debug(String.format("class map size: %d", this.classMap.size()));
    }

    private Map<String, byte[]> tryToLoadFromFile() {
        try {
            HashMap<String, byte[]> hashMap = new HashMap<String, byte[]>();
            String string = SQStructure.getProgramDirPath() + "internal/libs/SQLib.jar";
            try (JarFile jarFile = new JarFile(string);){
                Enumeration<JarEntry> enumeration = jarFile.entries();
                URL[] uRLArray = new URL[]{new URL("jar:file:" + string + "!/")};
                URLClassLoader uRLClassLoader = URLClassLoader.newInstance(uRLArray);
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = enumeration.nextElement();
                    if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) continue;
                    String string2 = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);
                    string2 = string2.replace('/', '.');
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    try {
                        byte[] byArray = IOUtils.toByteArray((InputStream)inputStream);
                        hashMap.put(string2, byArray);
                    }
                    finally {
                        if (inputStream == null) continue;
                        inputStream.close();
                    }
                }
            }
            return hashMap;
        }
        catch (Throwable throwable) {
            Log.error("Unable to load jar", throwable);
            return new HashMap<String, byte[]>();
        }
    }

    @Override
    public Iterable<Set<JavaFileManager.Location>> listLocationsForModules(JavaFileManager.Location location) throws IOException {
        return this.standardFileManager.listLocationsForModules(location);
    }

    @Override
    public String inferModuleName(JavaFileManager.Location location) throws IOException {
        return this.standardFileManager.inferModuleName(location);
    }

    private <T> T invokeNamedMethodIfAvailable(JavaFileManager.Location location, String string) {
        Method[] methodArray;
        for (Method method : methodArray = this.standardFileManager.getClass().getDeclaredMethods()) {
            if (!method.getName().equals(string) || method.getParameterTypes().length != 1 || method.getParameterTypes()[0] != JavaFileManager.Location.class) continue;
            try {
                return (T)method.invoke((Object)this.standardFileManager, location);
            }
            catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                throw new UnsupportedOperationException("Unable to invoke method " + string);
            }
        }
        throw new UnsupportedOperationException("Unable to find method " + string);
    }

    @Override
    public int isSupportedOption(String string) {
        return this.standardFileManager.isSupportedOption(string);
    }

    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return this.classLoader;
    }

    @Override
    public Iterable<JavaFileObject> list(JavaFileManager.Location location, String string, Set<JavaFileObject.Kind> set, boolean bl) throws IOException {
        List<JavaFileObject> list = this.tryToFind(string);
        if (!list.isEmpty()) {
            return list;
        }
        return this.standardFileManager.list(location, string, set, bl);
    }

    private List<JavaFileObject> tryToFind(String string) {
        LinkedList<JavaFileObject> linkedList = new LinkedList<JavaFileObject>();
        boolean bl = ALLOWED_PACKAGES_SET.contains(string);
        for (String string2 : this.classMap.keySet()) {
            String string3 = this.getPackageName(string2);
            if (!string.equals(string3) || !bl && !ALLOWED_CLASSES_SET.contains(string2)) continue;
            linkedList.add(new CustomJavaFileObject(string2, this.classMap.get(string2)));
        }
        return linkedList;
    }

    private String getPackageName(String string) {
        int n = string.lastIndexOf(".");
        return string.substring(0, n);
    }

    @Override
    public String inferBinaryName(JavaFileManager.Location location, JavaFileObject javaFileObject) {
        if (javaFileObject instanceof CustomJavaFileObject) {
            return ((CustomJavaFileObject)javaFileObject).binaryName();
        }
        return this.standardFileManager.inferBinaryName(location, javaFileObject);
    }

    @Override
    public boolean isSameFile(FileObject fileObject, FileObject fileObject2) {
        return this.standardFileManager.isSameFile(fileObject, fileObject2);
    }

    @Override
    public boolean handleOption(String string, Iterator<String> iterator) {
        return this.standardFileManager.handleOption(string, iterator);
    }

    @Override
    public boolean hasLocation(JavaFileManager.Location location) {
        return this.standardFileManager.hasLocation(location);
    }

    @Override
    public JavaFileObject getJavaFileForInput(JavaFileManager.Location location, String string, JavaFileObject.Kind kind) throws IOException {
        return this.standardFileManager.getJavaFileForInput(location, string, kind);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String string, JavaFileObject.Kind kind, FileObject fileObject) throws IOException {
        return this.standardFileManager.getJavaFileForOutput(location, string, kind, fileObject);
    }

    @Override
    public FileObject getFileForInput(JavaFileManager.Location location, String string, String string2) throws IOException {
        return this.standardFileManager.getFileForInput(location, string, string2);
    }

    @Override
    public FileObject getFileForOutput(JavaFileManager.Location location, String string, String string2, FileObject fileObject) throws IOException {
        return this.standardFileManager.getFileForOutput(location, string, string2, fileObject);
    }

    @Override
    public void flush() throws IOException {
        this.standardFileManager.flush();
    }

    @Override
    public void close() throws IOException {
        this.standardFileManager.close();
    }
}

