package com.strategyquant.tradinglib.project.websocket.channels.impl;

import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.channels.IProjectChannel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineChartsChannel implements IProjectChannel {
   private static final Logger Log = LoggerFactory.getLogger(EngineChartsChannel.class);
   private String lastData = "";

   @Override
   public Object getData(SQProject var1) {
      JSONObject var2 = var1.engineCharts.print();
      String var3 = var2.toString();
      if (var3.hashCode() == this.lastData.hashCode()) {
         return null;
      }

      this.lastData = var3;
      return var2;
   }

   @Override
   public String getChannelName() {
      return "engine-charts-channel";
   }
}
