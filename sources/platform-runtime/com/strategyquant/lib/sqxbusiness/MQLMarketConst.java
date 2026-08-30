/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.snippets.compile.SQStructure;

public class MQLMarketConst {
    public static final String BUILD_KEY_SETTINGS = "settings";
    public static final String PLATFORM_MT4 = "mql4";
    public static final String PLATFORM_MT5 = "mql5";
    public static final String PLATFORM_MT45 = "mql45";
    public static final String BUILDTYPE_ALL = "all";
    public static final String BUILDTYPE_UNLOCKED = "unlocked";
    public static final String BUILDTYPE_LOCKEDTODEMO = "lockedToDemo";
    public static final String BUILDTYPE_FIXEDSIZE = "fixedSize";
    public static final String BUILDTYPE_LOCKEDBYDATE = "lockedByDate";
    public static final String BUILDTYPE_DEMOLOCKEDBYDATE = "demoLockedByDate";
    public static final String BUILDTYPE_LOCKEDBYACCOUNT = "lockedByAccount";
    public static final String projectsPath = SQPaths.userDirPath + "/sqxbusiness";
    public static final String projectConfigFileName = "config.xml";
    public static final String mainConfigFileName = "config.xml";
    public static final String outputFolderName = "output";

    public static String getProjectDirPath(String string) {
        if (string.equals("Sample Project")) {
            return SQStructure.INTERNAL_DIR_PATH + "web/SQXBUSINESS/Sample Project";
        }
        return projectsPath + "/" + string;
    }

    public static String getBuildJobID(String string, String string2, boolean bl) {
        return String.format("%s/%s/%s", string, string2, bl ? "MT5" : "MT4");
    }

    public static String getProjectNameFromJobID(String string) {
        return string.split("/")[0];
    }
}

