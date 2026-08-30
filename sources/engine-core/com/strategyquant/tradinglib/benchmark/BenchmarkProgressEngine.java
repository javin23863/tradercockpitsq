package com.strategyquant.tradinglib.benchmark;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.project.ProgressEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkProgressEngine extends ProgressEngine {
   public static final Logger Log = LoggerFactory.getLogger("ProgressTracker");

   public BenchmarkProgressEngine() {
      super("Benchmark");
   }

   @Override
   public void setProgress(long var1) {
      int var3 = SQUtils.round(100.0 * ((double)var1 / this.maxProgress));
      super.setProgress(var3);
   }
}
