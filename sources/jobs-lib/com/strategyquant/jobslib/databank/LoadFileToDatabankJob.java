package com.strategyquant.jobslib.databank;

import com.strategyquant.jobslib.SQJob;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import java.io.File;

public class LoadFileToDatabankJob extends SQJob {
   private Databank databank;
   private File file;

   public LoadFileToDatabankJob(File var1, Databank var2) {
      this.file = var1;
      this.databank = var2;
   }

   @Override
   public void run() throws Exception {
      IProgram var1 = Program.get("Loader");
      ResultsGroup var2 = (ResultsGroup)var1.call("loadFile", new Object[]{this.file.getAbsolutePath()});
      this.databank.add(var2, true);
      this.databank.updateBestResults(var2);
   }
}
