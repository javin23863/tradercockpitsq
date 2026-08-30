package com.strategyquant.tradinglib.task.settings;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

public interface ISettingTabPlugin extends ISQPlugin {
   String getSettingName();

   String getName();

   void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4);

   void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception;

   JSONObject getInitializationData() throws Exception;
}
