/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;

public class LastUsedDirectory {
    public static final String LOAD_PROJECT = "loadProject";
    public static final String LOAD_RESULT = "loadResult";
    public static final String SAVE_RESULT = "saveResult";
    public static final String LOAD_TEMPLATE = "loadTemplate";
    public static final String SAVE_SOURCE_CODE = "saveSourceCode";
    public static final String LOAD_DATA_CONFIG = "loadDataConfig";
    public static final String SAVE_DATA_CONFIG = "saveDataConfig";
    public static final String LOAD_RESULT_TO_OPTIMIZE = "loadResultToOptimize";
    public static final String IMPORT_DATA = "importData";
    public static final String LOAD_PARAMETERS_CONFIG = "loadParametersConfig";
    public static final String SAVE_PARAMETERS_CONFIG = "saveParametersConfig";
    public static final String SAVE_FILTERS = "saveFilters";
    public static final String LOAD_FILTERS = "loadFilters";

    public static String getPath(String string) {
        String string2 = MainApp.settings().get(string);
        if (string2 == null) {
            if (string.equals(LOAD_PROJECT)) {
                string2 = SQPaths.projectsDirPath;
            }
            if (string.equals(LOAD_RESULT) || string.equals(SAVE_RESULT)) {
                string2 = SQPaths.projectsDirPath;
            } else if (string.equals(LOAD_TEMPLATE)) {
                string2 = SQPaths.bbTemplatesDirPath;
            } else if (string.equals(SAVE_SOURCE_CODE)) {
                string2 = SQPaths.strategySourcesDirPath;
            } else if (string.equals(LOAD_DATA_CONFIG) || string.equals(SAVE_DATA_CONFIG)) {
                string2 = SQPaths.dataConfigsDirPath;
            } else if (string.equals(LOAD_RESULT_TO_OPTIMIZE)) {
                string2 = SQPaths.projectsDirPath;
            } else if (string.equals(IMPORT_DATA)) {
                string2 = SQPaths.dataDirPath;
            } else if (string.equals(LOAD_PARAMETERS_CONFIG) || string.equals(SAVE_PARAMETERS_CONFIG)) {
                string2 = SQPaths.parametersConfigsPath;
            } else if (string.equals(SAVE_FILTERS) || string.equals(LOAD_FILTERS)) {
                string2 = SQPaths.filtersDirPath;
            }
        }
        if (string2 == null || !new File(string2).exists()) {
            string2 = SQPaths.userDirPath;
        }
        return string2;
    }

    public static void savePath(String string, String string2) {
        if (string2 == null || string2.trim().equals("")) {
            return;
        }
        MainApp.settings().set(string, string2);
    }
}

