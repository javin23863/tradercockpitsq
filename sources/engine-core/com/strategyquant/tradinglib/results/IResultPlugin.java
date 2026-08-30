package com.strategyquant.tradinglib.results;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import org.json.JSONObject;

public interface IResultPlugin extends IServletPlugin {
   boolean containsResult(ResultsGroup var1) throws Exception;

   String getKey();

   void setResultsGroupProvider(IResultsGroupProvider var1);

   JSONObject getInitializationData() throws Exception;
}
