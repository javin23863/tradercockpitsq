package com.strategyquant.tradinglib.results.file;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.ResultsGroup;

public interface ILoaderPlugin extends ISQPlugin {
   String[] getFileExtensions();

   ResultsGroup load(String var1, boolean var2, String var3, String var4) throws Exception;

   ResultsGroup finishLoad(ResultsGroup var1) throws Exception;
}
