package com.strategyquant.tradinglib.project;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.ProjectRunInfo;
import org.jdom2.Element;

public interface IProject extends ISQPlugin {
   Element setConfig() throws Exception;

   Element getConfig() throws Exception;

   Databanks getDatabanks();

   String getName();

   void start() throws Exception;

   void stop();

   void pause();

   void resume() throws Exception;

   int getRunningStatus();

   void setRunningStatus(int var1);

   int getRecentPos();

   void setRecentPos(int var1);

   void getTrackingInfo(ProjectRunInfo var1);
}
