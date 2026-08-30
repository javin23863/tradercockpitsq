package com.strategyquant.tradinglib.taskImpl;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import org.jdom2.Element;

public interface ISQTask extends ISQPlugin {
   String getType();

   String getName();

   String getPluginFolderName();

   String getCustomName();

   String getTitle();

   boolean isActive();

   void setActive(boolean var1);

   String[] getSettings();

   TaskSettingsData getSettingsData();

   void setCustomName(String var1);

   void setTitle(String var1);

   void setConfig(Element var1, boolean var2, boolean var3) throws Exception;

   Element getConfig();

   Element getDefaultConfig(String var1) throws Exception;

   ISQTask clone(String var1, ProgressEngine var2) throws Exception;

   void start() throws Exception;

   void afterFinish();

   void setTaskLogPrefix(String var1);

   String getErrorsDescription();

   void logTaskStarted(ProjectGlobalLog var1);

   void logTaskFinished(ProjectGlobalLog var1);

   void increaseNumberOfRuns();

   int getNumberOfRuns();

   int getIndex();

   void setIndex(int var1);

   void setConfigFileName(String var1);

   String getTaskXMLFileName();

   double getCustomValue(String var1);
}
