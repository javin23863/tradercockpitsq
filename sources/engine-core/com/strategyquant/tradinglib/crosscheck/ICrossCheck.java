package com.strategyquant.tradinglib.crosscheck;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.io.Serializable;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.jdom2.Element;

public interface ICrossCheck extends ISQPlugin, Serializable {
   String CROSSCHECK_PREFIX = "CrossCheck_";
   String CROSSCHECK_HIGHER_PRECISION_KEY = "CrossCheck_HigherPrecision";
   String CROSSCHECK_WHAT_IF_KEY = "CrossCheck_WhatIf";
   String CROSSCHECK_SEQUENTIAL_OPTIMIZATION_KEY = "CrossCheck_SequentialOptimization";

   ICrossCheck clone(SettingsMap var1);

   int getType();

   String getName();

   String getShortName();

   SettingsMap getSettings();

   String getSettingName();

   String getDescription();

   boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception;

   void fixSettings(Element var1);

   void readSettings(Element var1, TaskSettingsData var2) throws Exception;

   int getDismissalReason();

   String getDismissalMessage();

   void setStopPauseEngine(StopPauseEngine var1);

   void registerProgressListener(IBacktestProgressListener var1);

   int getNumberOfSimulations();

   boolean doesRetest();

   boolean doesForEverySetup();

   double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception;

   String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception;

   String getColumnTitle(String var1, Element var2, Object... var3);

   String getColumnTitleTemplate();

   void saveDataToFile(ResultsGroup var1, JarOutputStream var2) throws Exception;

   void loadDataFromFile(ResultsGroup var1, JarFile var2) throws Exception;

   ChartSetups getChartSetups(ChartSetup var1);

   boolean doesCreateSubjobs();

   boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception;

   String printSettings(Element var1) throws Exception;

   int getBadStrategyReason();
}
