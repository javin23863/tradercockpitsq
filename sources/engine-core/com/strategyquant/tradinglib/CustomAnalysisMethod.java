package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.debug.Debugger;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CustomAnalysisMethod extends Debugger {
   public static final Logger Log = LoggerFactory.getLogger(CustomAnalysisMethod.class);
   public static final int TYPE_PROCESS_DATABANK = 10;
   public static final int TYPE_FILTER_STRATEGY = 20;
   public static final int TYPE_BOTH = 30;
   public String name;
   private int type;
   protected String projectLogMessage;
   private String inputArgs;

   public CustomAnalysisMethod(String var1, int var2) {
      this.name = var1;
      this.type = var2;
   }

   public int getType() {
      return this.type;
   }

   public boolean filterStrategy(String var1, String var2, String var3, ResultsGroup var4) throws Exception {
      return true;
   }

   public ArrayList<ResultsGroup> processDatabank(String var1, String var2, String var3, ArrayList<ResultsGroup> var4) throws Exception {
      return var4;
   }

   public void setProjectLog(String var1) {
      this.projectLogMessage = var1;
   }

   public String getProjectLog() {
      return this.projectLogMessage;
   }

   public String getInputArgs() {
      return this.inputArgs;
   }

   public void setInputArgs(String var1) {
      this.inputArgs = var1;
   }
}
