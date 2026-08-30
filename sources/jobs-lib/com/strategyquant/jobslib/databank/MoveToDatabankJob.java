package com.strategyquant.jobslib.databank;

import com.strategyquant.jobslib.SQJob;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;

public class MoveToDatabankJob extends SQJob {
   private ResultsGroup resultsGroup;
   private Databank databank;

   public MoveToDatabankJob(ResultsGroup var1, Databank var2) {
      this.resultsGroup = var1;
      this.databank = var2;
   }

   @Override
   public void run() throws Exception {
   }
}
