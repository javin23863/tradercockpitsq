package com.strategyquant.tradinglib.project;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.generator.StrategyTemplateGenerator;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.options.TradingOptionsList;
import com.strategyquant.tradinglib.propertygrid.IPGParameter;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyXMLModifier {
   private static final Logger Log = LoggerFactory.getLogger(StrategyXMLModifier.class);
   private static final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
   public static final String MM_FROM_STRATEGY = "fromStrategy";

   public static Element getUpdatedStrategyXML(ResultsGroup var0) {
      return getUpdatedStrategyXML(var0, "fromStrategy", null);
   }

   public static Element getUpdatedStrategyXML(ResultsGroup var0, String var1, String var2) {
      try {
         return getUpdatedStrategyXML(var0, var0.portfolio().getStrategyXml().clone(), var1, var0.getName(), var0.getLastSettings(), var2);
      } catch (Exception var4) {
         Log.error("Cannot update strategy XML from last settings (2).", var4);
         return null;
      }
   }

   public static Element getUpdatedStrategyXML(ResultsGroup var0, Element var1, String var2, String var3, String var4, String var5) {
      try {
         addOptions(var1, var3, var5);
         addLastBacktestInfo(var0, var1);
         if (var4 != null) {
            Element var6 = XMLUtil.stringToElement(var4);
            if (var6 != null) {
               try {
                  var1.removeChild("TradingOptions");
                  Element var7 = var6.getChild("Options").getChild("BuildTradingOptions").clone();
                  var7.setName("TradingOptions");
                  var1.addContent(var7);
               } catch (Exception var17) {
                  Log.warn("Cannot read trading options from last settings of strategy '{}'", var3);
               }

               try {
                  Element var20 = var1.getChild("Strategy");
                  var20.removeChild("MoneyManagement");
                  Element var8 = getMoneyManagement(var6.getChild("RiskMoneyManagement").getChild("MoneyManagement"), var2);
                  var20.addContent(var8);
                  updateChartSettings(var1, var6);
               } catch (Exception var16) {
                  Log.warn("Cannot read money management from last settings of strategy '{}'", var3);
               }

               try {
                  Element var21 = var1.getChild("options");
                  Element var24 = var21.getChild("MinDistance");
                  Element var9 = var6.getChild("Data").getChild("Setups").getChild("Setup");
                  double var10 = Double.parseDouble(var9.getAttributeValue("minDist"));
                  var24.setText("" + var10);
                  String var12 = var9.getAttributeValue("engine");
                  Element var13 = var21.getChild("BacktestEngine");
                  if (var13 == null) {
                     var13 = new Element("BacktestEngine");
                     var21.addContent(var13);
                  }

                  var13.setText(var12);
               } catch (Exception var15) {
                  Log.warn("Cannot read min distance from last settings of strategy '{}'", var3);
               }

               try {
                  Element var22 = var1.getChild("ATMs");
                  if (var22 == null) {
                     var22 = new Element("ATMs");
                     var1.addContent(var22);
                  }

                  addMandatoryEmptyATM(var22);
               } catch (Exception var14) {
                  Log.warn("Cannot read trading options from last settings of strategy '{}'", var3);
               }
            } else {
               Log.warn("Cannot get last settings of strategy '{}'", var3);
               createDefaultMMSettings(var1, var2);
            }
         } else {
            Log.warn("No last settings available for strategy '{}'", var3);
            createDefaultMMSettings(var1, var2);
         }

         Element var19 = XMLUtil.tryAddElement(var1, "AvailableTradingOptions");
         var19.removeContent();
         List var23 = TradingOptionsList.getInstance().getAvailableClasses();

         for (int var25 = 0; var25 < var23.size(); var25++) {
            TradingOption var26 = (TradingOption)var23.get(var25);
            if (!var26.isInbuild()) {
               var19.addContent(new Element("TradingOption").setText(var26.getClass().getSimpleName()));
            }
         }
      } catch (Exception var18) {
         Log.error("Cannot update strategy XML from last settings.", var18);
      }

      return var1;
   }

   public static void addMandatoryEmptyATM(Element var0) {
      List var1 = var0.getChildren("ATM");
      if (var1 == null || var1.isEmpty()) {
         Element var2 = new Element("ATM");
         var2.setAttribute("empty", "true");
         var0.addContent(var2);
         var0.addContent(var2.clone());
         var0.setAttribute("enable", "false");
      }
   }

   private static void updateChartSettings(Element var0, Element var1) {
      try {
         Element var2 = var1.getChild("Data");
         Element var3 = var2.getChild("Setups").getChild("Setup");
         Element var4 = var0.getChild("Strategy").getChild("Datas");
         List var5 = var4.getChildren("data");
         List var6 = var3.getChildren("Chart");
         Element var7 = (Element)var6.get(0);
         String var8 = var7.getAttributeValue("symbol");

         for (int var9 = 0; var9 < var6.size(); var9++) {
            Element var10 = (Element)var6.get(var9);
            String var11 = var10.getAttributeValue("symbol");
            Element var12 = null;
            if (var9 < var5.size()) {
               var12 = (Element)var5.get(var9);
               Element var13 = var12.getChild("symbol");
               var13.setText(var9 == 0 ? "NULL" : var11);
               Element var14 = var12.getChild("timeFrame");
               var14.setText(var9 == 0 ? "0" : String.valueOf(TimeframeManager.getMillis(var10.getAttributeValue("timeframe")) / 60000L));
            } else {
               var12 = new Element("data");
               Element var19 = new Element("id").setText(String.valueOf(var9));
               Element var20 = new Element("symbol").setText(var9 == 0 ? "NULL" : var11);
               Element var15 = new Element("chart").setText(var9 == 0 ? "Main chart" : "Subchart " + var9);
               Element var16 = new Element("timeFrame")
                  .setText(var9 == 0 ? "0" : String.valueOf(TimeframeManager.getMillis(var10.getAttributeValue("timeframe")) / 60000L));
               var12.addContent(var19).addContent(var20).addContent(var15).addContent(var16);
               var4.addContent(var12);
            }

            StrategyTemplateGenerator.addTickSizeToDataElem(var12, var11, var8);
         }
      } catch (Exception var17) {
         Log.error("Updating data settings failed", var17);
      }
   }

   public static void addOptions(Element var0, String var1, String var2) {
      Element var3 = var0.getChild("options");
      if (var3 != null) {
         Element var17 = var3.getChild("ID");
         if (var17 == null) {
            var3.addContent(new Element("ID").setText(getID()));
         }

         Element var18 = var3.getChild("MinDistance");
         if (var18 != null) {
            try {
               Double.parseDouble(var18.getText());
            } catch (Exception var13) {
               var18.setText("0");
            }
         } else {
            var3.addContent(new Element("MinDistance").setText("0"));
         }

         Element var6 = var3.getChild("StrategyName");
         if (var6 == null) {
            var3.addContent(new Element("StrategyName").setText(var1));
         } else {
            var6.setText(var1);
         }

         Element var7 = var3.getChild("Version");
         if (var7 != null) {
            var7.setText(MainApp.printAppVersion(false));
         } else {
            try {
               var3.addContent(new Element("Version").setText(MainApp.printAppVersion(false)));
            } catch (Exception var12) {
               var3.addContent(new Element("Version").setText("N/A"));
            }
         }

         Element var8 = var3.getChild("Date");
         Date var9 = new Date();
         if (var8 != null) {
            var8.setText(dateFormat.format(var9));
         } else {
            try {
               var3.addContent(new Element("Date").setText(dateFormat.format(var9)));
            } catch (Exception var11) {
               var3.addContent(new Element("Date").setText("N/A"));
            }
         }
      } else {
         var3 = new Element("options");
         var0.addContent(0, var3);
         var3.addContent(new Element("StrategyName").setText(var1));

         try {
            var3.addContent(new Element("StrategyClassName").setText(SQUtils.correctClassName(SQUtils.stripExtension(new File(var2).getName()))));
         } catch (Exception var15) {
            var3.addContent(new Element("StrategyClassName").setText(SQUtils.correctClassName(var1)));
         }

         String var4 = var0.getChild("Strategy").getAttributeValue("engine");
         var3.addContent(new Element("Engine").setText(var4));
         var3.addContent(new Element("OnlineVersion").setText("false"));

         try {
            var3.addContent(new Element("Version").setText(MainApp.printAppVersion(false)));
         } catch (Exception var14) {
            var3.addContent(new Element("Version").setText("N/A"));
         }

         Date var5 = new Date();
         var3.addContent(new Element("Date").setText(dateFormat.format(var5)));
         var3.addContent(new Element("ID").setText(getID()));
         var3.addContent(new Element("MinDistance").setText("0"));
      }
   }

   private static String getID() {
      return RandomStringUtils.randomAlphabetic(8);
   }

   private static void addLastBacktestInfo(ResultsGroup var0, Element var1) {
      if (var0 != null && var0.specialValues() != null) {
         long var2 = var0.specialValues().getLong(SpecialValues.HistoryFrom, 0L);
         long var4 = var0.specialValues().getLong(SpecialValues.HistoryTo, 0L);
         String var6 = null;

         try {
            var6 = var0.mainResult().getString(SpecialValues.Symbol, null);
         } catch (Exception var11) {
         }

         String var7 = null;

         try {
            var7 = var0.mainResult().getString(SpecialValues.Timeframe, null);
         } catch (Exception var10) {
         }

         Element var8 = var1.getChild("options");
         if (var8 != null) {
            Element var9 = var8.getChild("Backtest");
            if (var9 == null) {
               var9 = new Element("Backtest");
               var8.addContent(var9);
            }

            var9.removeContent();
            XMLUtil.tryAddNode(var9, "DateFrom", var2 > 0L ? SQTime.formatDate(var2) : "??");
            XMLUtil.tryAddNode(var9, "DateTo", var4 > 0L ? SQTime.formatDate(var4) : "??");
            XMLUtil.tryAddNode(var9, "Symbol", var6 != null ? var6 : "??");
            XMLUtil.tryAddNode(var9, "Timeframe", var7 != null ? var7 : "??");
         }
      }
   }

   private static Element getMoneyManagement(Element var0, String var1) throws Exception {
      Element var2 = var0.clone();
      var2.removeContent();
      Element var3 = new Element("Method");
      var2.addContent(var3);
      Element var4 = new Element("Params");
      var3.addContent(var4);
      if (var1.equals("fromStrategy")) {
         List var5 = var0.getChildren("Method");
         if (var5.size() == 1) {
            var2.setAttribute("type", addMMParams(var4, (Element)var5.get(0)));
         } else {
            for (int var6 = 0; var6 < var5.size(); var6++) {
               Element var7 = (Element)var5.get(var6);
               if (XMLUtil.elementIs(var7, "use")) {
                  var2.setAttribute("type", addMMParams(var4, var7));
                  break;
               }
            }
         }
      } else {
         var2.setAttribute("type", addMMParams(var4, var1));
      }

      String var8 = var0.getChildText("InitialCapital");
      var8 = var8 != null ? var8 : "10000";
      Element var10 = new Element("InitialCapital");
      var10.setText(var8);
      var2.addContent(var10);
      return var2;
   }

   private static void createDefaultMMSettings(Element var0, String var1) throws Exception {
      if (var1.equals("fromStrategy")) {
         if (hasValidMMSettings(var0)) {
            return;
         }

         var1 = "FixedSize";
      }

      Element var2 = var0.getChild("Strategy");
      var2.removeChild("MoneyManagement");
      Element var3 = new Element("MoneyManagement");
      var3.setAttribute("type", var1);
      Element var4 = new Element("InitialCapital");
      var4.setText("10000");
      var3.addContent(var4);
      Element var5 = new Element("Method");
      var5.setAttribute("type", var1);
      var5.setAttribute("use", "true");
      Element var6 = new Element("Params");
      addMMParams(var6, var1);
      var5.addContent(var6);
      var3.addContent(var5);
      var2.addContent(var3);
   }

   private static boolean hasValidMMSettings(Element var0) {
      try {
         Element var1 = var0.getChild("Strategy");
         Element var2 = var1.getChild("MoneyManagement");
         Element var3 = var2.getChild("Method");
         Element var4 = var3.getChild("Params");
         if (var4 != null) {
            return true;
         }
      } catch (Exception var5) {
         Log.error("Strategy contains invalid MM settings", var5);
      }

      return false;
   }

   private static String addMMParams(Element var0, String var1) throws Exception {
      MoneyManagementMethod var2 = (MoneyManagementMethod)MoneyManagementMethodsList.get().findClassByName(var1);
      addMMParams(var0, var2);
      return var2.getClass().getSimpleName();
   }

   private static String addMMParams(Element var0, Element var1) throws Exception {
      MoneyManagementMethod var2 = null;

      try {
         var2 = MoneyManagementMethodsList.createFromXml(var1);
         addMMParams(var0, var2);
      } catch (Exception var4) {
         Log.error("Incorrect MoneyManagement settings, using FixedSize...");
         var2 = (MoneyManagementMethod)MoneyManagementMethodsList.get().findClassByName("FixedSize");
         addMMParams(var0, var2);
      }

      return var2 != null ? var2.getClass().getSimpleName() : null;
   }

   public static void addMMParams(Element var0, MoneyManagementMethod var1) throws Exception {
      Element var2 = var1.getXML().getChild("Method");

      for (Element var5 : var2.getChild("Params").getChildren("Param")) {
         String var6 = var5.getAttributeValue("key");
         boolean var7 = false;

         for (IPGParameter var9 : var1.getParams()) {
            if (var9.getKey().equals(var6)) {
               String var10 = String.valueOf(var1.getPGParameterValue(var6));
               Element var11 = new Element("Param");
               var11.setAttribute("key", "#" + var6 + "#");
               var11.setAttribute("varname", "null");
               var11.setAttribute("type", printMMParamType(var9.getType()));
               var11.setAttribute("value", var10);
               var11.setText(var10);
               var0.addContent(var11);
               var7 = true;
               break;
            }
         }

         if (!var7) {
            Log.error("PG Parameter '" + var6 + "' not found");
         }
      }
   }

   private static String printMMParamType(int var0) {
      switch (var0) {
         case 1:
            return "int";
         case 2:
            return "double";
         case 3:
            return "boolean";
         case 4:
            return "int";
         case 5:
            return "string";
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         default:
            return "string";
         case 10:
            return "int";
         case 20:
            return "int";
         case 30:
            return "string";
      }
   }
}
