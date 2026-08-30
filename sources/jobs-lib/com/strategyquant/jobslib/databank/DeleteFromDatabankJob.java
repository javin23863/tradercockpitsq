package com.strategyquant.jobslib.databank;

import com.strategyquant.jobslib.SQJob;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;

public class DeleteFromDatabankJob extends SQJob {
   private ResultsGroup resultsGroup;
   private Databank databank;

   public DeleteFromDatabankJob(ResultsGroup var1, Databank var2) {
      this.resultsGroup = var1;
      this.databank = var2;
   }

   @Override
   public void run() throws Exception {
   }
}
