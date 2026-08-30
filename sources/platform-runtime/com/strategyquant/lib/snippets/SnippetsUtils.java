/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.SnippetsUtilsException;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.SnippetsHash;
import com.strategyquant.lib.snippets.compile.SnippetsHashException;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnippetsUtils {
    public static final Logger Log = LoggerFactory.getLogger(SnippetsUtils.class);
    private static final String TAG_SNIPPETS_BUILTIN_DIR = "$SNIPPETS_BUILTIN_DIR$";
    private static final String TAG_SNIPPETS_USER_DIR = "$SNIPPETS_USER_DIR$";

    private SnippetsUtils() {
    }

    public static String normalizePath(String string) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put(SQStructure.getSnippetsBuiltinDirPath(), "$SNIPPETS_BUILTIN_DIR$/");
        hashMap.put(SQStructure.getSnippetsUserDirPath(), "$SNIPPETS_USER_DIR$/");
        return SQUtils.remapFilePathByMap(string, hashMap);
    }

    public static String denormalizePath(String string) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("$SNIPPETS_BUILTIN_DIR$/", SQStructure.getSnippetsBuiltinDirPath());
        hashMap.put("$SNIPPETS_USER_DIR$/", SQStructure.getSnippetsUserDirPath());
        return SQUtils.remapFilePathByMap(string, hashMap);
    }

    public static void synchronizeSnippets() throws SnippetsUtilsException {
        block12: {
            File file = new File(SQStructure.SNIPPETS_JAR_PATH);
            String[] stringArray = SQStructure.getSnippetsSourceDirs();
            try {
                if (!file.exists()) break block12;
                try (ZipFile zipFile = new ZipFile(file, 1);){
                    Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                    while (enumeration.hasMoreElements()) {
                        String string;
                        ZipEntry zipEntry = enumeration.nextElement();
                        if (zipEntry.isDirectory() || (string = zipEntry.getName().replace("class", "java")).contains("$") || string.endsWith("Abstract")) continue;
                        boolean bl = false;
                        for (String string2 : stringArray) {
                            if (!new File(string2 + string).exists()) continue;
                            bl = true;
                            break;
                        }
                        if (bl) continue;
                        Log.info("Snippets are not synchronized. Missing source file '{}'.", (Object)string);
                        Log.info("Deleting hash file to recompile snippets.");
                        SnippetsHash.deleteFilesHashFile();
                        return;
                    }
                }
            }
            catch (Exception exception) {
                SnippetsUtilsException snippetsUtilsException = new SnippetsUtilsException("Cannot synchronize snippets.", exception);
                try {
                    SnippetsHash.deleteFilesHashFile();
                }
                catch (SnippetsHashException snippetsHashException) {
                    snippetsUtilsException.addSuppressed(snippetsHashException);
                }
                throw snippetsUtilsException;
            }
        }
    }

    public static void removeOldSnippets() throws SnippetsUtilsException {
        try {
            File file = new File(SQStructure.getSnippetsBuiltinDirPath() + "SQ/RobustnessTests");
            if (file.exists()) {
                FileUtils.deleteDirectory((File)file);
            }
        }
        catch (IOException iOException) {
            throw new SnippetsUtilsException("Error removing obsolete snippets", iOException);
        }
    }
}

