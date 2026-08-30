package com.strategyquant.jobslib.databank;

import com.strategyquant.jobslib.SQJob;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;

public class AddToDatabankJob extends SQJob {
   private ResultsGroup resultsGroup;
   private Databank databank;

   public AddToDatabankJob(ResultsGroup var1, Databank var2) {
      this.resultsGroup = var1;
      this.databank = var2;
   }

   @Override
   public void run() throws Exception {
      ResultsGroup var1 = this.resultsGroup.clone();
      this.databank.add(var1, true);
      this.databank.updateBestResults(var1);
   }
}
