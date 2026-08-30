package com.strategyquant.tradinglib.databank;

import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;

public interface IDatabankFilter {
   void init(Databank var1) throws Exception;

   void init(Databank var1, IProgressListener var2) throws Exception;

   void destroy();

   int getMaxStrategies();

   double getWorstFitness();

   boolean changeMaxRecords(int var1, IProgressListener var2) throws Exception;

   void onRemove(String var1);

   boolean onAdd(ResultsGroup var1);

   void onUpdate(ResultsGroup var1);

   boolean checkShouldAdd(ResultsGroup var1);
}
