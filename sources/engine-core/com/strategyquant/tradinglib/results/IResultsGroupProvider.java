package com.strategyquant.tradinglib.results;

import com.strategyquant.tradinglib.ResultsGroup;
import java.util.Map;

public interface IResultsGroupProvider {
   ResultsGroup get(Map<String, String[]> var1, String var2) throws Exception;

   String getFilePath(ResultsGroup var1) throws Exception;
}
