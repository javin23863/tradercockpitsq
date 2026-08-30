package com.strategyquant.tradinglib.plugindef.app;

import com.strategyquant.pluginlib.ISQPlugin;

public interface IAppPlugin extends ISQPlugin {
   String getName();

   String getContextPath();

   String getAppCode();

   String getTooltip();

   String getProject();

   String getDefaultTaskType();

   String getDefaultTaskName();
}
