package com.strategyquant.tradinglib.project.websocket.channels;

import com.strategyquant.tradinglib.project.websocket.channels.impl.BestResultsChannel;
import com.strategyquant.tradinglib.project.websocket.channels.impl.DatabankChannel;
import com.strategyquant.tradinglib.project.websocket.channels.impl.EngineChannel;
import com.strategyquant.tradinglib.project.websocket.channels.impl.EngineChartsChannel;
import com.strategyquant.tradinglib.project.websocket.channels.impl.LogChannel;
import com.strategyquant.tradinglib.project.websocket.channels.impl.ProgressChannel;
import java.util.ArrayList;

public class ProjectChannels {
   public static final String PROGRESS_CHANNEL = "progress-channel";
   public static final String DATABANKS_CHANNEL = "databanks-channel";
   public static final String ENGINE_CHANNEL = "engine-channel";
   public static final String ENGINE_CHARTS_CHANNEL = "engine-charts-channel";
   public static final String LOG_CHANNEL = "log-channel";
   public static final String BEST_RESULTS_CHANNEL = "best-results-channel";
   private static ArrayList<IProjectChannel> channels = new ArrayList<>();
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         initialized = true;
         channels.add(new DatabankChannel());
         channels.add(new EngineChannel());
         channels.add(new EngineChartsChannel());
         channels.add(new ProgressChannel());
         channels.add(new LogChannel());
         channels.add(new BestResultsChannel());
      }
   }

   public static ArrayList<IProjectChannel> getChannels() {
      return channels;
   }
}
