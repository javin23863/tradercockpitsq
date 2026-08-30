package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuildResult;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuildSettings;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuilder;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQLMarketBuildJob extends GridJob<MQLMarketBuildResult> {
   private static final Logger Log = LoggerFactory.getLogger("MQLMarketBuildJob");
   private MQLMarketBuildSettings buildSettings;
   private MQLMarketBuilder builder;

   public MQLMarketBuildJob(String var1, int var2, Map<String, Serializable> var3) {
      super(var1, var2, var3);
      this.buildSettings = (MQLMarketBuildSettings)var3.get("settings");
      this.builder = new MQLMarketBuilder(var1);
   }

   public MQLMarketBuildResult call() throws Exception {
      SQXBusinessBuildReporter.printToLog(this.getJobId(), L.t("Starting build...", new Object[0]));
      SQXBusinessBuildReporter.setStatus(this.getJobId(), 1);
      return this.builder.build(this.buildSettings, SQXBusinessBuildReporter.get());
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 2) {
         SQXBusinessBuildReporter.printToLog(this.getJobId(), L.t("Build paused", new Object[0]));
         this.builder.pause();
      } else if (var1.getMessageID() == 4) {
         SQXBusinessBuildReporter.printToLog(this.getJobId(), L.t("Build stopped", new Object[0]));
         this.builder.stop();
      } else if (var1.getMessageID() == 3) {
         SQXBusinessBuildReporter.printToLog(this.getJobId(), L.t("Build resumed", new Object[0]));
         this.builder.resume();
      }
   }
}
