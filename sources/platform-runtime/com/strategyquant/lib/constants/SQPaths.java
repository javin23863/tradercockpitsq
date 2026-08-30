/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.constants;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;

public class SQPaths {
    public static final String userDirPath = MainApp.getDataPath() + "user";
    public static final String dataDirPath = userDirPath + "/data";
    public static final String customDataDirPath = userDirPath + "/customdata";
    public static final String strategiesDirPath = userDirPath + "/strategies";
    public static final String strategySourcesDirPath = userDirPath + "/strategy_sources";
    public static final String projectsDirPath = userDirPath + "/projects";
    public static final String exportDirPath = MainApp.getDataPath() + "export";
    public static final String bbTemplatesDirPath = userDirPath + "/templates";
    public static final String settingsDirPath = userDirPath + "/settings";
    public static final String testsDirPath = MainApp.getDataPath() + "tests";
    public static final String pluginsDirPath = MainApp.getDataPath() + "internal/plugins";
    public static final String filtersDirPath = settingsDirPath + "/filters";
    public static final String configsDirPath = settingsDirPath + "/Configs";
    public static final String buildConfigDirPath = settingsDirPath + "/BuildConfig";
    public static final String strategyTemplatesDirPath = settingsDirPath + "/StrategyTemplates";
    public static final String appSettingsFile = settingsDirPath + "/settings.xml";
    public static final String remoteSettingsFile = settingsDirPath + "/remote_access.xml";
    public static final String dataConfigsDirPath = settingsDirPath + "/data_configs";
    public static final String parametersConfigsPath = settingsDirPath + "/parameters_configs";
    public static final String wizardConfigPath = SQStructure.INTERNAL_DIR_PATH + "web/SQWIZARD/branding/global/config.xml";
    public static final String buildTemplatesDirPath = SQStructure.INTERNAL_DIR_PATH + "web/BUILDER/templates";
    public static final String simpleTemplatesDirPath = SQStructure.INTERNAL_DIR_PATH + "web/BUILDER/simpleTemplates";
    public static final String indicatorTestsPath = testsDirPath + "/Indicators";
    public static final String algoWizardStrategyPath = MainApp.getDataPath() + "internal/web/AlgoWizard/strategy.sqx";
    public static final String algoWizardBlockGroupsPath = settingsDirPath + "/blockGroups.xml";
    public static final String algoWizardCustomBlocksPath = settingsDirPath + "/customBlocks.xml";
    public static final String wfDefaultConditionsPath = pluginsDirPath + "/SettingsRankings/WFDefaultConditions.xml";
    public static final String algoWizardCustomDataIndysPath = settingsDirPath + "/customDataIndys.xml";
}

