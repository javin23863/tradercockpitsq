/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSUtils {
    private static final Logger Log = LoggerFactory.getLogger(OSUtils.class);
    public static boolean isWindows;
    public static boolean isX64;
    private static Process p;

    public static InputStream executeCommand(String[] stringArray) {
        String string = "";
        for (int i = 0; i < stringArray.length; ++i) {
            string = string + stringArray[i];
            if (i >= stringArray.length - 1) continue;
            string = string + " ";
        }
        Log.info("Executing command '" + string + "'...");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(stringArray);
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput();
            p = processBuilder.start();
            return p.getInputStream();
        }
        catch (IOException iOException) {
            Log.error("Error while executing command.", (Throwable)iOException);
            return null;
        }
    }

    public static String executeAndRead(String[] stringArray) {
        char[] cArray = new char[128];
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = OSUtils.executeCommand(stringArray);
        if (inputStream != null) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);){
                int n;
                while ((n = ((Reader)inputStreamReader).read(cArray, 0, cArray.length)) >= 0) {
                    stringBuilder.append(cArray, 0, n);
                }
                inputStream.close();
                ((Reader)inputStreamReader).close();
            }
            catch (Exception exception) {
                Log.error("Error while reading the output of a command.", (Throwable)exception);
            }
            p.destroy();
            return stringBuilder.toString();
        }
        return null;
    }

    public static void addLibraryPath(String string) {
        String string2 = System.getProperty("java.library.path") + ";" + string;
        System.setProperty("java.library.path", string2);
        try {
            Field field = ClassLoader.class.getDeclaredField("sys_paths");
            field.setAccessible(true);
            field.set(null, null);
            Log.info("Java Library Path: " + System.getProperty("java.library.path"));
        }
        catch (Exception exception) {
            Log.error("Setting library path failed.", (Throwable)exception);
        }
    }

    public static Process getProcess() {
        return p;
    }

    static {
        String string = System.getProperty("os.name");
        isWindows = string != null && string.toLowerCase().contains("windows");
        String string2 = System.getenv("PROCESSOR_ARCHITECTURE");
        String string3 = System.getenv("PROCESSOR_ARCHITEW6432");
        isX64 = string2 != null && string2.endsWith("64") || string3 != null && string3.endsWith("64");
    }
}

