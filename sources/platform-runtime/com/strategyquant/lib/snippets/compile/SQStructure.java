/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQStructure {
    public static final Logger Log = LoggerFactory.getLogger((String)"SQStructure");
    public static final String EXTEND_DIR_PATH = MainApp.getDataPath() + "internal/extend/";
    public static final String PACKAGE_FIRST_SUBPATH = "SQ/";
    public static final String PACKAGE_PATH = "SQ/";
    public static final String SNIPPETS_DIR_PATH_WITHOUT_PACKAGE = EXTEND_DIR_PATH + "Snippets/";
    public static final String SNIPPETS_DIR_PATH = SNIPPETS_DIR_PATH_WITHOUT_PACKAGE + "SQ/";
    public static final String CODE_DIR_PATH = EXTEND_DIR_PATH + "Code/";
    public static final String TEMP_DIR_PATH = MainApp.getDataPath() + "temp/";
    public static final String INTERNAL_DIR_PATH = MainApp.getDataPath() + "internal/";
    public static final String COMPILED_DIR_PATH_WITHOUT_PACKAGE = INTERNAL_DIR_PATH + "compiled/";
    public static final String COMPILED_DIR_PATH = COMPILED_DIR_PATH_WITHOUT_PACKAGE + "SQ/";
    public static final String SNIPPETS_JAR_PATH = MainApp.getDataPath() + "internal/libs/Snippets.jar";
    public static final String SNIPPETS_HASH_PATH = SQPaths.settingsDirPath + "/snippets.txt";
    public static final String WIZARD_HASH_PATH = SQPaths.settingsDirPath + "/wizard.txt";
    public static final String PLUGINS_DIR = MainApp.getDataPath() + "internal/plugins/";
    public static final String LIBS_DIR = MainApp.getDataPath() + "internal/libs/";
    public static final String JAVA_J64 = MainApp.getDataPath() + "j64/";
    public static final String JAVA_J32 = MainApp.getDataPath() + "j32/";
    public static final String JAVA_J64_LIB_DIR = JAVA_J64 + "jre/lib/";
    public static final String JAVA_J32_LIB_DIR = JAVA_J32 + "jre/lib/";
    public static final String SQ4_PROJECTS_DIR = MainApp.isRelease() ? null : MainApp.getDataPath() + "../../projects/strategyquant/";
    public static final String SQ4_LIBS_DIR = MainApp.isRelease() ? null : MainApp.getDataPath() + "../../projects/libs/";
    public static final String ORIGINAL_ZIP_NAME = "extend.zip";
    public static final String ORIGINAL_EXTENDS_ZIP = INTERNAL_DIR_PATH + "extend.zip";
    public static final String ORIGINAL_SNIPPET_PACKAGE = "internal/extend/Snippets/com/";
    public static final String LOG_DIR = MainApp.getDataPath() + "log/";
    public static final String UPDATE_INFO_DIR_NAME = "info";
    public static final String UPDATES_DIR_PATH = INTERNAL_DIR_PATH + "updates/";
    public static final String UPDATES_INSTALL_FILE = INTERNAL_DIR_PATH + "updates/install.txt";
    public static final String USER_LIBS = MainApp.getDataPath() + "user/libs";
    public static final String USER_EXTEND = MainApp.getDataPath() + "user/extend";
    public static final String USER_EXTEND_SNIPPETS = MainApp.getDataPath() + "user/extend/Snippets";
    public static final String USER_EXTEND_PLUGINS = MainApp.getDataPath() + "user/extend/Plugins";
    public static final String USER_EXTEND_RESULTS_PLUGINS = MainApp.getDataPath() + "user/extend/ResultsPlugins";
    public static final String RESULTS_CUSTOM_PLUGIN = "CustomPlugin";
    public static final String RESULTS_PROP_ANALYTICS = "Prop analytics";
    public static final String RESULTS_PROP_MONTE_CARLO = "Prop Monte Carlo";
    public static final String PROFILE_CHART_DIR = MainApp.getDataPath() + "internal/tmp/profilechart";

    public static String getUpdateDirPath(String string) {
        return UPDATES_DIR_PATH + string + "/";
    }

    public static String getCompiledCustomDirPath(String string) {
        return COMPILED_DIR_PATH + string + "/";
    }

    private SQStructure() {
    }

    public static String getAbsoluteDirPath(String string) {
        String string2 = SQStructure.getAbsoluteFilePath(string);
        return string2.endsWith("/") ? string2 : string2 + '/';
    }

    public static String getAbsoluteFilePath(String string) {
        String string2 = string;
        File file = new File(string);
        string2 = file.getAbsolutePath();
        string2 = string2.replace("\\", "/");
        return string2;
    }

    public static String getProgramDirPath() {
        return SQStructure.getAbsoluteDirPath(MainApp.getDataPath());
    }

    public static String getExtendBuiltinDirPath() {
        return SQStructure.getAbsoluteDirPath(SQStructure.getProgramDirPath() + "internal/extend");
    }

    public static String getSnippetsBuiltinDirPath() {
        return SQStructure.getAbsoluteDirPath(SQStructure.getExtendBuiltinDirPath() + "Snippets");
    }

    public static String getSnippetsBuiltinInternalDirPath() {
        return SQStructure.getAbsoluteDirPath(SQStructure.getSnippetsBuiltinDirPath() + "SQ/Internal");
    }

    public static String getExtendUserDirPath() {
        String string = SQStructure.getAbsoluteDirPath(SQStructure.getProgramDirPath() + "user/extend");
        string = MainApp.settings().get("userExtendAltDir", string);
        return SQStructure.getAbsoluteDirPath(string);
    }

    public static String getSnippetsUserDirPath() {
        return SQStructure.getAbsoluteDirPath(SQStructure.getExtendUserDirPath() + "Snippets");
    }

    public static String[] getSnippetsSourceDirs() {
        return new String[]{SQStructure.getSnippetsBuiltinDirPath(), SQStructure.getSnippetsUserDirPath()};
    }

    public static Map<String, String> getSnippetsSourceDirsAsMap() {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        linkedHashMap.put(SQStructure.getSnippetsBuiltinDirPath(), "Builtin");
        linkedHashMap.put(SQStructure.getSnippetsUserDirPath(), "User");
        return linkedHashMap;
    }

    public static String[] getSnippetsSourceDirsWithSuffix(String string) {
        String[] stringArray = SQStructure.getSnippetsSourceDirs();
        String[] stringArray2 = new String[stringArray.length];
        for (int i = 0; i < stringArray.length; ++i) {
            stringArray2[i] = SQStructure.getAbsoluteDirPath(stringArray[i] + string);
        }
        return stringArray2;
    }

    public static Map<String, String> getExtendsSourceDirsAsMap(String[] stringArray) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < stringArray.length; ++i) {
            if (stringArray[i].equals("Builtin")) {
                linkedHashMap.put(SQStructure.getExtendBuiltinDirPath(), "Builtin");
                continue;
            }
            if (!stringArray[i].equals("User")) continue;
            linkedHashMap.put(SQStructure.getExtendUserDirPath(), "User");
        }
        return linkedHashMap;
    }

    public static String getIndyCodeBuiltinDirPath() {
        return SQStructure.getAbsoluteDirPath(SQStructure.getExtendBuiltinDirPath() + "Code");
    }

    public static String getIndyCodeUserDirPath() {
        return SQStructure.getAbsoluteDirPath(SQStructure.getExtendUserDirPath() + "Code");
    }

    public static String[] getIndyCodeDirs() {
        return new String[]{SQStructure.getIndyCodeBuiltinDirPath(), SQStructure.getIndyCodeUserDirPath()};
    }
}

