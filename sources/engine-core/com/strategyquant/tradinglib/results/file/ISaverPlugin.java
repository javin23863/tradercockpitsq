package com.strategyquant.tradinglib.results.file;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.Map;

public interface ISaverPlugin extends ISQPlugin {
   String[] getFileExtensions();

   void save(ResultsGroup var1, String var2, Map<String, String[]> var3) throws Exception;

   ISaverPlugin clone();
}
