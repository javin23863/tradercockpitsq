package com.strategyquant.tradinglib.project;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.settings.ParametrizationSettings;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.results.file.FileHandler;
import com.strategyquant.tradinglib.results.file.ISaverPlugin;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategySaver {
   public static final Logger Log = LoggerFactory.getLogger(StrategySaver.class);
   private int index = 0;
   private IProgram saver = new FileHandler();

   public void resetCounting() {
      this.index = 0;
   }

   public void save(ResultsGroup var1, String var2, String var3, boolean var4, int var5) throws Exception {
      this.save(var1, var2, var3, var4, var5, null);
   }

   public void save(ResultsGroup var1, String var2, String var3, boolean var4, int var5, Map<String, String[]> var6) throws Exception {
      this.save(var1, var2, var3, var4, var5, null, var6, false);
   }

   public void save(ResultsGroup var1, String var2, String var3, boolean var4, int var5, SourceCodeGenerator var6, Map<String, String[]> var7, boolean var8) throws Exception {
      boolean var9 = false;
      if (var7 != null && var7.containsKey("extension[description]")) {
         var9 = ((String[])var7.get("extension[description]"))[0].toLowerCase().contains("brazil")
            || ((String[])var7.get("extension[description]"))[0].toLowerCase().contains("brasil");
      }

      if (var6 != null) {
         var9 = var6.isBrazilianVersion();
      }

      if (var2 == null) {
         throw new Exception("File format (extension) cannot be null.");
      }

      switch (var2) {
         case "sq4":
         case "sqx":
            this.saveStrategy(var1, var3, var4, var5, var7);
            break;
         case "xml":
            this.saveConfig(var1, var3);
            break;
         case "txt":
            this.saveCode(var1, var3, SourceCodeGenerator.PseudoCode, var4, var5);
            break;
         case "mq4":
            this.saveCode(var1, var3, SourceCodeGenerator.MetaTrader4, var4, var5);
            break;
         case "mq5":
            if (var9) {
               this.saveCode(var1, var3, SourceCodeGenerator.MetaTrader5BR, var4, var5);
            } else {
               this.saveCode(var1, var3, SourceCodeGenerator.MetaTrader5, var4, var5);
            }
            break;
         case "java":
            this.saveCode(var1, var3, SourceCodeGenerator.JForex, var4, var5);
            break;
         case "el":
            this.saveELCode(var1, var3, var4, var5, false);
            break;
         case "pla":
            this.saveELCode(var1, var3, var4, var5, true);
            break;
         default:
            this.saveUsingPlugins(var1, var2, var3, var7, var8);
      }
   }

   private void saveUsingPlugins(ResultsGroup var1, String var2, String var3, Map<String, String[]> var4, boolean var5) throws Exception {
      Exception var6 = null;

      for (ISaverPlugin var10 : SQPluginManager.getPlugins(ISaverPlugin.class)) {
         String var7 = SQPluginManager.getPluginName(var10);

         try {
            String[] var11 = var10.getFileExtensions();
            if (var11 != null && var11.length > 0) {
               for (int var12 = 0; var12 < var11.length; var12++) {
                  if (var11[var12].equalsIgnoreCase(var2)) {
                     if (var5) {
                        var10 = var10.clone();
                     }

                     var10.save(var1, var3, var4);
                     return;
                  }
               }
            }
         } catch (Exception var13) {
            var6 = var13;
            if (Log.isDebugEnabled()) {
               Log.debug(" - Save using {} failed with exception: {}", var7, var13.getMessage());
            }
         }
      }

      if (var6 == null) {
         throw new Exception("No plugin found for saving strategy to '" + var2 + "'.");
      } else {
         throw new Exception(var6.getMessage());
      }
   }

   private void saveStrategy(ResultsGroup var1, String var2, boolean var3, int var4, Map<String, String[]> var5) throws Exception {
      if (var3) {
         ResultsGroup var6 = var1.clone();
         Element var7 = var6.getStrategyXml();
         Element var8 = XMLUtil.getChildElem(var7, "Strategy");
         Element var9 = XMLUtil.getChildElem(var8, "Variables");
         List var10 = var9.getChildren("variable");

         for (int var11 = 0; var11 < var10.size(); var11++) {
            Element var12 = (Element)var10.get(var11);
            String var13 = XMLUtil.getChildElem(var12, "name").getText();
            if (var13 != null && var13.equals("MagicNumber")) {
               XMLUtil.getChildElem(var12, "value").setText("" + (var4 + this.index++));
            }
         }

         var6.portfolio().addStrategyXml(var7);
         this.saver.call("saveFile", new Object[]{var6, var2, true, var5});
      } else {
         this.saver.call("saveFile", new Object[]{var1, var2, true, var5});
      }
   }

   private void saveStrategy(Element var1, String var2) throws Exception {
      this.saver.call("saveStrategyFile", new Object[]{var1, var2, true});
   }

   private void saveConfig(ResultsGroup var1, String var2) throws Exception {
      AbstractBroker var3 = MainApp.v571hfnsHw().inygRmSMx7();
      if (var3 != null && var3.usesEAEncryption()) {
         throw new Exception(L.t("Action not supported for current broker", new Object[0]));
      }

      Element var4 = StrategyXMLModifier.getUpdatedStrategyXML(var1);
      SQUtils.stringToFile(var2, XMLUtil.elementToString(var4));
   }

   private void saveCode(ResultsGroup var1, String var2, String var3, boolean var4, int var5) throws Exception {
      String var6 = this.getSourceCode(var1, var3, var2);
      if (var4) {
         int var7 = 0;
         int var8 = 0;
         String var9 = "int MagicNumber";

         while ((var8 = var6.indexOf(var9, var7)) >= 0) {
            var7 = var8 + 1;
            int var10 = var6.indexOf(" = ", var8);
            if (var10 >= 0) {
               int var11 = var6.indexOf(";", var10);
               if (var11 >= 0) {
                  var6 = var6.substring(0, var10 + 3) + (var5 + this.index++) + var6.substring(var11);
               }
            }
         }
      }

      SQUtils.stringToFile(var2, var6);
   }

   private void saveELCode(ResultsGroup var1, String var2, boolean var3, int var4, boolean var5) throws Exception {
      String var6 = this.getSourceCode(var1, SourceCodeGenerator.EasyLanguage, var2);
      if (var3) {
         int var7 = 0;
         int var8 = 0;
         String var9 = "MagicNumber(";

         while ((var8 = var6.indexOf(var9, var7)) >= 0) {
            var7 = var8 + var9.length();
            int var10 = var6.indexOf(")", var7);
            if (var10 < 0) {
               throw new Exception("MagicNumber param end bracket not found");
            }

            var6 = var6.substring(0, var7) + (var4 + this.index++) + var6.substring(var10);
         }
      }

      File var11 = new File(var2);
      if (var5) {
         String var13 = SQUtils.stripExtension(var11.getName());
         var6 = getPLAContent(var13, var6);
      }

      SQUtils.stringToFile(var11, var6);
   }

   public static String getPLAContent(String var0, String var1) {
      var0 = var0 == null ? "SQStrategy" : var0.replaceAll("[^A-Za-z0-9_]", "");
      String var2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n<Graph>\r\n\t<GraphNode>\r\n\t\t<data first=\"s_"
         + var0
         + "\" second=\"7\"/>\r\n\t\t<signature ObligatoryParamCount=\"0\" RetType=\"0\" StorageType=\"32\"/>\r\n\t\t<StudyProperties BarRefMode=\"1\" BarRefValue=\"50\" SaveAsSymbol=\"0\" SemilogAxis=\"1\" SkipIdentTicks=\"1\" UpdateOnEveryTick=\"1\"/>\r\n\t\t<PasswordState IsProtectedByPassword=\"0\" Password=\"1B2M2Y8AsgTpgAmY7PhCfg==\"/>\r\n\t\t<NodeText Encoded=\"1\">";
      String var3 = Base64.getEncoder().encodeToString(var1.getBytes());

      for (byte var4 = 76; var4 < var3.length(); var4 += 77) {
         var3 = var3.substring(0, var4) + "\n" + var3.substring(var4);
      }

      return var2 + var3 + "</NodeText>\r\n\t</GraphNode>\r\n</Graph>";
   }

   private String getSourceCode(ResultsGroup var1, String var2, String var3) throws Exception {
      AbstractBroker var4 = MainApp.v571hfnsHw().inygRmSMx7();
      if (var4 != null && var4.usesEAEncryption()) {
         throw new Exception(L.t("Action not supported for current broker", new Object[0]));
      }

      Element var5 = StrategyXMLModifier.getUpdatedStrategyXML(var1, "fromStrategy", var3);
      if (isMetaTraderGenerator(var2)) {
         normalizeExportSymbols(var5);
      }

      try {
         String var6 = ParametrizationSettings.getSettingKey(var2);
         if (MainApp.settings().containsKey(var6)) {
            JSONObject var7 = new JSONObject(MainApp.settings().get(var6));
            ValuesMap var8 = new ValuesMap();
            var8.set("ParamTypePeriod", var7.getBoolean("periodParams"));
            var8.set("ParamTypeShift", var7.getBoolean("shiftParams"));
            var8.set("ParamTypeExitUsed", var7.getBoolean("exitParamsUsed"));
            var8.set("ParamTypeConstant", var7.getBoolean("constantsParams"));
            var8.set("ParamTypeOtherParam", var7.getBoolean("otherParams"));
            var8.set("ParamTypeEntryLevel", var7.getBoolean("entryParams"));
            var8.set("ParamTypeEntryLogic", var7.getBoolean("entryLogic"));
            var8.set("ParamTypeExitUnused", var7.getBoolean("exitParamsUnused"));
            var8.set("ParamTypeBoolean", var7.getBoolean("booleanParams"));
            var8.set("ParamTypeRecommended", var7.getBoolean("recommendedParams"));
            boolean var9 = false;

            try {
               var9 = Boolean.parseBoolean(MainApp.settings().get("sourceCodeVariables"));
            } catch (Exception var18) {
            }

            boolean var10 = true;

            try {
               var10 = Boolean.parseBoolean(MainApp.settings().get("sourceCodeSymmetry"));
            } catch (Exception var17) {
            }

            StrategyBase var11 = StrategyBase.createXmlStrategy(var5);
            if (var9) {
               var11.transformToVariables(var10, var8);
               var11.variables().sortByName();
            } else {
               var11.transformToNumbers();
            }
         }
      } catch (Exception var19) {
         Log.error("Failed to apply parametrization settings", var19);
      }

      double var20 = 1.0;
      double var21 = 0.0;
      if (!var2.equals(SourceCodeGenerator.StrategyXML) && !var2.equals(SourceCodeGenerator.PseudoCode)) {
         if (var1.isStockpickerStrategy()) {
            throw new Exception(String.format("Stockpicker strategies are not supported for platform '%s'.", var2));
         }

         try {
            String var22 = var1.getLastSettings();
            if (var22 != null) {
               Element var24 = XMLUtil.stringToElement(var22);
               Element var12 = var24.getChild("Data").getChild("Setups").getChild("Setup");
               Element var13 = XMLUtil.getChildElem(var12, "Chart");
               String var14 = var13.getAttributeValue("symbol");
               InstrumentInfo var15 = DataManager.getDataInfo("History", var14).symbolInfo;
               var20 = var15.orderSizeMultiplier;
               var21 = var15.orderSizeStep;
            }
         } catch (Exception var16) {
            Log.error("Exc.", var16);
         }
      }

      XMLIndicatorsAnalyzer.init();
      XMLIndicatorsAnalyzer.fixItemAttributes(var5);
      CustomDataManager.init(SQPaths.customDataDirPath);
      CustomDataManager.addCDataIndySourceCodes(var5);
      CustomBlocks.addCBlockSourceCodes(var5);
      SourceCodeGenerator var23 = SourceCodeGenerators.getInstance().getGeneratorFromName(var2);
      return var23.getSource(var1.getName(), var5, var20, var21);
   }

   public static boolean isMetaTraderGenerator(String var0) {
      return var0.equals(SourceCodeGenerator.MetaTrader4) || var0.equals(SourceCodeGenerator.MetaTrader5) || var0.equals(SourceCodeGenerator.MetaTrader5BR);
   }

   public static void normalizeExportSymbols(Element var0) {
      try {
         Element var1 = var0.getChild("Strategy").getChild("Datas");
         if (var1 == null) {
            return;
         }

         List var2 = var1.getChildren("data");
         if (var2 == null || var2.isEmpty()) {
            return;
         }

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            Element var5 = var4.getChild("symbol");
            if (var5 != null) {
               String var6 = var5.getText();
               if (var6 != null && !var6.equals("NULL") && !var6.equals("Current")) {
                  if (var3 == 0) {
                     var5.setText("NULL");
                  } else {
                     try {
                        DataInfo var7 = DataManager.getDataInfo("History", var6);
                        if (var7 != null && var7.instrument != null && !var7.instrument.isEmpty() && !var7.instrument.equals(var6)) {
                           var5.setText(var7.instrument);
                        }
                     } catch (Exception var8) {
                        Log.info("Cannot resolve instrument name for subchart symbol '{}', keeping as is", var6);
                     }
                  }
               }
            }
         }
      } catch (Exception var9) {
         Log.error("Failed to normalize chart symbols for MT export", var9);
      }
   }
}
