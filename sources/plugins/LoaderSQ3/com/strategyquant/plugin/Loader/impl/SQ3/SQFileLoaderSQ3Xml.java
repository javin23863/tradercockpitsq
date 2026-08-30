package com.strategyquant.plugin.Loader.impl.SQ3;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.file.MissingInstrumentException;
import com.strategyquant.tradinglib.strategy.xml.SQ3StrategyConverter;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SQFileLoaderSQ3Xml {
   public static final Logger Log = LoggerFactory.getLogger("SQFileLoaderSQ3Xml");

   public void load(ResultsGroup var1, Element var2, boolean var3, String var4) throws Exception {
      Element var5 = var2.getChild("Strategy");
      Element var6 = null;
      if (var5 != null) {
         if (var5.getAttributeValue("note") != null) {
            var1.specialValues().setString(SpecialValues.Note, var5.getAttributeValue("note"));
         }

         var6 = SQ3StrategyConverter.getInstance().convert(var5);
      }

      Element var7 = var2.getChild("TestResults");
      SettingsMap var8 = parseSettings(var7);
      String var9 = var8.getString("Symbol").intern();
      String var10 = var8.getString("Symbol") + "-" + TimeframeManager.translateSQ3TFToString(var8.getInt("Timeframe")).intern();
      this.buildLastSettingsXml(var1, var8);
      this.addOrdersAndSetup(var1, var7.getChild("Orders"), var9, var10, var3, var4);
      Element var11 = var7.getChild("AdditionalDataResults");
      if (var11 != null) {
         for (Element var13 : var11.getChild("AdResults").getChildren()) {
            SettingsMap var14 = parseAdSettings(var13);
            var9 = var14.getString("Symbol").intern();
            var10 = var14.getString("Symbol") + "-" + TimeframeManager.translateSQ3TFToString(var14.getInt("Timeframe")).intern();
            this.addOrdersAndSetup(var1, var13.getChild("Orders"), var9, var10, var3, var4);
         }
      }

      var1.specialValues().setString(SpecialValues.Symbol, var8.getString("Symbol"));
      SQTimeOld var17 = (SQTimeOld)var8.get("DateFrom");
      var1.specialValues().set(SpecialValues.HistoryFrom, var17.getMilis());
      var17 = (SQTimeOld)var8.get("DateTo");
      var1.specialValues().set(SpecialValues.HistoryTo, var17.getMilis());
      var1.specialValues().setString(SpecialValues.Timeframe, TimeframeManager.translateSQ3TFToString(var8.getInt("Timeframe")));
      if (var6 != null) {
         var1.portfolio().addStrategyXml(var6);
      }
   }

   private void buildLastSettingsXml(ResultsGroup var1, SettingsMap var2) {
      try {
         ISQTask var3 = null;

         for (ISQTask var5 : SQPluginManager.getPlugins(ISQTask.class)) {
            if (var5.getType().equals("Build")) {
               var3 = var5;
               break;
            }
         }

         if (var3 == null) {
            return;
         }

         Element var17 = var3.getDefaultConfig("task_forex.xml").getChild("Settings");
         Element var18 = var17.getChild("Data");
         Element var6 = var18.getChild("Setups");
         Element var7 = var6.getChild("Setup");
         Element var8 = var7.getChild("Chart");
         var7.setAttribute("dateFrom", SQTime.toUIDateString(((SQTimeOld)var2.get("DateFrom")).getDateInMs()));
         var7.setAttribute("dateTo", SQTime.toUIDateString(((SQTimeOld)var2.get("DateTo")).getDateInMs()));
         String var9 = var2.get("testPrecision") + "";
         var7.setAttribute("testPrecision", var9);
         var7.setAttribute("realSpread", (Integer.parseInt(var9) == 4) + "");
         var7.setAttribute("slippage", var2.get("slippage") + "");
         var7.setAttribute("minDist", var2.get("minDistance") + "");
         var8.setAttribute("symbol", var2.get("symbol") + "");
         var8.setAttribute("timeframe", TimeframeManager.translateSQ3TFToString(Integer.parseInt(var2.get("timeframe") + "")));
         var8.setAttribute("spread", var2.get("spread") + "");
         Element var10 = var17.getChild("RiskMoneyManagement");
         Element var11 = var10.getChild("MoneyManagement");
         Element var12 = var11.getChild("InitialCapital");
         var12.setText(var2.get("MoneyManagement.InitialCapital") + "");
         Element var13 = var17.getChild("Rankings");
         Element var14 = var13.getChild("MaxStrategies");
         var14.setText(var2.get("maxRecordsInDatabank") + "");
         String var15 = XMLUtil.elementToString(var17);
         var1.setLastSettings(var15);
      } catch (Exception var16) {
         Log.error("Cannot load settings of strategy. Exc.", var16);
      }
   }

   private void addOrdersAndSetup(ResultsGroup var1, Element var2, String var3, String var4, boolean var5, String var6) throws Exception {
      if (var1.hasResult(var4)) {
         var4 = var1.generateUniqueResultKeyName(var4);
      }

      var1.addSubresult(var4, new SettingsMap());
      int var7 = 0;
      OrdersList var8 = new OrdersList("addOrddersAndSetup");
      HashMap var9 = new HashMap();

      for (Element var11 : var2.getChildren()) {
         Order var12 = OrderParser.parse(var11, "2.0");
         var12.Symbol = var3;
         var12.SetupName = var4;
         var12.Order = var7++;
         var8.add(var12);
      }

      if (var8.size() > 0) {
         new Order();

         for (int var13 = 0; var13 < var8.size(); var13++) {
            Order var15 = var8.get(var13);
            if (!var9.containsKey(var15.Symbol.hashCode())) {
               var9.put(var15.Symbol.hashCode(), var15.Symbol);
            }
         }

         for (String var16 : var9.values()) {
            InstrumentInfo var17;
            if (!InstrumentManager.checkInstrumentExists(var16)) {
               if (var5) {
                  var17 = InstrumentManager.findClosestInstrument(var16);
                  if (var17 == null) {
                     throw new MissingInstrumentException(var16);
                  }
               } else if (var6 == null) {
                  var17 = InstrumentManager.findClosestInstrument(var16);
               } else {
                  var17 = InstrumentManager.getInstrumentInfo(var6);
               }
            } else {
               var17 = InstrumentManager.getInstrumentInfo(var16);
            }

            var1.symbols().add(var16, var17.instrument, var17);
         }

         var1.orders().addAll(var8);
      }

      var8.clear();
   }

   private static SettingsMap parseAdSettings(Element var0) {
      SettingsMap var1 = new SettingsMap();
      var1.set("Symbol", var0.getChild("symbol").getValue());
      var1.set("Timeframe", XMLUtil.getInt(var0, "timeframe", 1));
      var1.set("DateFrom", new SQTimeOld(XMLUtil.getLong(var0, "dateFrom", 0) * 1000L));
      var1.set("DateTo", new SQTimeOld(XMLUtil.getLong(var0, "dateTo", 0) * 1000L));
      var1.set("MoneyManagement.InitialCapital", XMLUtil.getDouble(var0, "capitalSize", 10000.0));
      var1.set("TestPrecision", var0.getChild("testPrecision").getValue());
      var1.set("Spread", var0.getChild("spread").getValue());
      var1.set("Slippage", var0.getChild("slippage").getValue());
      var1.set("MinDistance", var0.getChild("minDistance").getValue());
      return var1;
   }

   private static SettingsMap parseSettings(Element var0) {
      SettingsMap var1 = new SettingsMap();
      Element var2 = var0.getChild("Results");
      if (var2 == null) {
         return null;
      }

      for (Element var5 : var2.getChildren()) {
         if (var5.getName().equalsIgnoreCase("SettingsFile")) {
            Element var6 = var5.getChild("Settings");
            parseParameters(var1, var6);
            var1.set("Symbol", var6.getChild("symbol").getValue());
            var1.set("Timeframe", XMLUtil.getInt(var6, "timeframe", 1));
            var1.set("DateFrom", new SQTimeOld(XMLUtil.getLong(var6, "dateFrom", 0) * 1000L));
            var1.set("DateTo", new SQTimeOld(XMLUtil.getLong(var6, "dateTo", 0) * 1000L));
            var1.set("MoneyManagement.InitialCapital", XMLUtil.getDouble(var6, "capitalSize", 10000.0));
            break;
         }
      }

      return var1;
   }

   private static void parseParameters(SettingsMap var0, Element var1) {
      for (Element var5 : var1.getChildren()) {
         String var2 = var5.getName();
         String var3 = parseSettingValue(var5.getName(), var5.getValue());
         if (var3 != null) {
            var0.set(var2, var3);
         }
      }
   }

   public static String parseSettingValue(String var0, String var1) {
      if (var0.equalsIgnoreCase("CustomConditions")) {
         return null;
      }

      if (var0.equalsIgnoreCase("generationType")) {
         int var2 = Integer.parseInt(var1);
         if (var2 == 1) {
            var1 = "GenerationRandom";
         } else if (var2 == 2) {
            var1 = "GenerationFromDatabank";
         }
      } else if (var0.equalsIgnoreCase("tradingSides")) {
         int var3 = Integer.parseInt(var1);
         if (var3 == 1) {
            var1 = "OnlyLong";
         } else if (var3 == 2) {
            var1 = "OnlyShort";
         } else if (var3 == 3) {
            var1 = "BothLong&Short";
         }
      } else if (var0.equalsIgnoreCase("selectionType")) {
         int var4 = Integer.parseInt(var1);
         if (var4 == 0) {
            var1 = "RouletteWheelSelection";
         } else if (var4 == 1) {
            var1 = "SigmaScaling";
         } else if (var4 == 2) {
            var1 = "StochasticUniversalSampling";
         } else if (var4 == 3) {
            var1 = "Truncationselection";
         }
      } else if (var0.equalsIgnoreCase("dateFrom")
         || var0.equalsIgnoreCase("dateTo")
         || var0.equalsIgnoreCase("oosDateFrom")
         || var0.equalsIgnoreCase("oosDateTo")
         || var0.equalsIgnoreCase("tradeTimeFrom")
         || var0.equalsIgnoreCase("tradeTimeTo")) {
         new SQTimeOld();
         var1 = SQTimeOld.toString(Integer.parseInt(var1));
      } else if (var0.equalsIgnoreCase("timeframe")) {
         int var5 = Integer.parseInt(var1);
      } else if (var0.equalsIgnoreCase("testingMode")) {
         int var6 = Integer.parseInt(var1);
         if (var6 == 1000) {
            var1 = "ModeEvolution";
         } else if (var6 == 2000) {
            var1 = "ModeRandom";
         } else if (var6 == 3000) {
            var1 = "ModeTest";
         } else if (var6 == 4000) {
            var1 = "ModeOptimize";
         }
      } else if (var0.equalsIgnoreCase("testPrecision")) {
         int var7 = Integer.parseInt(var1);
      } else if (var0.equalsIgnoreCase("buildGoal")) {
         int var8 = Integer.parseInt(var1);
         if (var8 == 1) {
            var1 = "GoalComplete";
         } else if (var8 == 3) {
            var1 = "GoalPartial";
         }
      } else if (var0.equalsIgnoreCase("mmType")) {
         int var9 = Integer.parseInt(var1);
         if (var9 == 0) {
            var1 = "Fixed";
         } else if (var9 == 1) {
            var1 = "RiskFixedPerc";
         }
      }

      return var1;
   }
}
