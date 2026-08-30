package com.strategyquant.tradinglib.generator;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.blocks.random.RandomValueConfig;
import com.strategyquant.tradinglib.simulator.Engines;
import java.io.File;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyTemplateGenerator {
   private static final Logger Log = LoggerFactory.getLogger("StrategyTemplateGenerator");
   private String strategyType;
   private String taskName;
   private int engine;
   private Element elSettings;
   private IRandomGenerator rng;
   private boolean useSameIdForLongShort = true;
   private String architecture;

   public StrategyTemplateGenerator(String var1, Element var2, IRandomGenerator var3) throws GenerateException {
      this.taskName = var1;
      this.elSettings = var2;
      this.rng = var3;
      Element var4 = var2.getChild("WhatToBuild");
      this.engine = getEngine(var2);
      this.strategyType = var4.getChild("StrategyType").getAttributeValue("type");
      this.architecture = var4.getChild("StrategyType").getAttributeValue("architecture");
      if (this.architecture == null) {
         this.architecture = "sq4";
      }
   }

   public static int getEngine(Element var0) {
      Element var1 = var0.getChild("Data");
      String var2 = var1.getChild("Setups").getChild("Setup").getAttributeValue("engine");
      return Engines.getEngine(var2);
   }

   public Element generate() throws Exception {
      IStrategyTemplate var1 = null;
      if (!this.strategyType.equals("multi-tf") && !this.strategyType.equals("simple")) {
         if (this.strategyType.equals("improve")) {
            if (this.engine == 1316847364 || this.engine == -1816889229) {
               throw new Exception(String.format("Improver is not yet implemented for Stockpicker strategies"));
            }

            this.architecture = this.recognizeArchitectureFromStrategy();
            if (this.architecture != null) {
               if (this.architecture.equals("sq3")) {
                  var1 = new StrategyImproveTemplateSQ3(this.elSettings, this.rng);
               } else if (this.architecture.equals("sq4")) {
                  var1 = new StrategyImproveTemplateSQ4(this.elSettings, this.rng);
               } else if (this.architecture.equals("sq4fuzzy")) {
                  var1 = new StrategyImproveTemplateSQ4(this.elSettings, this.rng);
               }
            }
         } else if (this.strategyType.equals("template")) {
            Element var3 = getConfiguredTemplate(this.elSettings, this.taskName);
            if (this.engine == 1316847364 || this.engine == -1816889229) {
               StrategyStockpickerTemplate.setExitsProbability(var3, this.elSettings, this.rng);
            }

            this.fixXmlns(var3);
            this.fixCharts(this.elSettings, var3);
            this.fixRandomGroups(var3);
            this.fixGeneratedBlocks(var3);
            return var3;
         }
      } else if (this.engine == 1316847364) {
         var1 = new StrategyStockpickerTemplate(this.elSettings, this.rng);
      } else if (this.engine == -1816889229) {
         var1 = new StrategyStockpickerSingleAssetTemplate(this.elSettings, this.rng);
      } else if (this.architecture.equals("sq3")) {
         var1 = new StrategyStandardTemplateSQ3(this.elSettings, this.rng);
      } else if (this.architecture.equals("sq4")) {
         var1 = new StrategyStandardTemplateSQ4(this.elSettings, this.rng);
      } else if (this.architecture.equals("sq4fuzzy")) {
         var1 = new StrategyStandardTemplateSQ4Fuzzy(this.elSettings, this.rng);
      }

      if (var1 == null) {
         throw new Exception(String.format("Unrecognized Strategy Type '%s' and architecture '%s'", this.strategyType, this.architecture));
      }

      var1.setUseSameIdForLongShort(this.useSameIdForLongShort);
      Element var2 = var1.generate();
      this.saveEngine(var2);
      return var2;
   }

   private void fixGeneratedBlocks(Element var1) {
      var1.setNamespace(Namespace.NO_NAMESPACE);
      String var2 = var1.getAttributeValue("generated");
      if (var2 != null) {
         var1.removeAttribute("generated");
         var1.removeAttribute("randomId");
      }

      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         this.fixGeneratedBlocks((Element)var3.get(var4));
      }
   }

   private void saveEngine(Element var1) {
      Element var2 = var1.getChild("Strategy");
      var2.setAttribute("engine", Engines.getEngineName(this.engine));
   }

   private void fixCharts(Element var1, Element var2) throws Exception {
      List var3 = ((Element)var1.getChild("Data").getChild("Setups").getChildren("Setup").get(0)).getChildren("Chart");
      Element var4 = var2.getChild("Strategy").getChild("Datas");
      if (var4.getChildren().size() > var3.size()) {
         throw new Exception(L.t("Strategy from template file requires %d charts, %d defined.", new Object[]{var4.getChildren().size(), var3.size()}));
      }

      var4.removeContent();
      String var5 = null;

      for (int var6 = 0; var6 < var3.size(); var6++) {
         Element var7 = (Element)var3.get(var6);
         Element var8 = new Element("data");
         var8.addContent(new Element("id").setText(Integer.toString(var6)));
         if (var6 == 0) {
            var8.addContent(new Element("chart").setText("Main chart"));
            var5 = var7.getAttributeValue("symbol");
            var8.addContent(new Element("symbol").setText("NULL"));
            var8.addContent(new Element("timeFrame").setText("0"));
         } else {
            var8.addContent(new Element("chart").setText(String.format("Subchart %d", var6)));
            if (var7.getAttributeValue("symbol").equals(var5)) {
               var8.addContent(new Element("symbol").setText("NULL"));
            } else {
               var8.addContent(new Element("symbol").setText(var7.getAttributeValue("symbol")));
            }

            var8.addContent(new Element("timeFrame").setText(StrategyStandardTemplateSQ3.convertTimeframeToWizard(var7.getAttributeValue("timeframe"))));
         }

         var4.addContent(var8);
      }
   }

   private void fixXmlns(Element var1) {
      var1.setNamespace(Namespace.NO_NAMESPACE);
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         this.fixXmlns((Element)var2.get(var3));
      }
   }

   private void fixRandomGroups(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("RandomGroups");
      if (var2 != null) {
         List var3 = var2.getChildren("Group");

         for (int var4 = 0; var4 < var3.size(); var4++) {
            this.fixRG((Element)var3.get(var4));
         }
      }
   }

   private void fixRG(Element var1) {
      String var2 = var1.getName();
      if (var2.equals("Param")) {
         var1.removeAttribute("identification");
         String var3 = var1.getAttributeValue("randomValue");
         if (var3 != null && !var3.equals("default")) {
            RandomValueConfig var4 = RandomValueConfig.parseRandomValue(var3, true);
            if (var4 == null) {
               Log.info("Template fix - randomValue attribute has incorrect format: '{}', setting to default", var3);
               var1.setAttribute("randomValue", "default");
            }
         }
      } else if (var2.equals("Item")) {
         List var7 = var1.getChildren("Param");

         for (int var9 = 0; var9 < var7.size(); var9++) {
            Element var5 = (Element)var7.get(var9);
            String var6 = var5.getAttributeValue("key");
            if (var6.equals("#Identification#")) {
               var1.removeContent(var5);
               break;
            }
         }
      }

      List var8 = var1.getChildren();

      for (int var10 = 0; var10 < var8.size(); var10++) {
         this.fixRG((Element)var8.get(var10));
      }
   }

   private String recognizeArchitectureFromStrategy() throws Exception {
      Element var1 = this.elSettings.getChild("WhatToBuild");
      String var2 = var1.getChild("StrategyType").getAttributeValue("strategyFile");
      if (!var2.contains("/") && !var2.contains("\\")) {
         var2 = MainApp.getDataPath() + "user/strategies/" + var2;
      }

      Log.info("Load strategy file {}", var2);
      File var3 = new File(var2);
      if (!var3.exists()) {
         throw new Exception(String.format("Improver - Strategy '%s' doesn't exist!", var2));
      }

      Element var4;
      if (var2.endsWith(".xml")) {
         var4 = XMLUtil.fileToXmlElement(var3);
      } else {
         IProgram var5 = Program.get("Loader");
         ResultsGroup var6 = (ResultsGroup)var5.call("loadFile", new Object[]{var2, false});
         var4 = var6.getStrategyXml();
      }

      if (var4 == null) {
         throw new Exception(String.format("Improver - No strategy found in file '%s' !", var2));
      }

      Element var10 = var4.getChild("Strategy");
      List var11 = var10.getChild("Rules").getChild("Events").getChildren();

      for (int var7 = 0; var7 < var11.size(); var7++) {
         if (((Element)var11.get(var7)).getAttributeValue("key").equals("OnBarUpdate")) {
            List var8 = ((Element)var11.get(var7)).getChildren("Rule");
            if (var8.size() >= 1) {
               Element var9 = (Element)var8.get(0);
               if (var9.getAttributeValue("type").equals("Signal")) {
                  return "sq4";
               }

               if (var9.getAttributeValue("type").equals("SignalFuzzy")) {
                  return "sq4fuzzy";
               }

               return "sq3";
            }
            break;
         }
      }

      return null;
   }

   public static Element getConfiguredTemplate(Element var0, String var1) throws Exception {
      Element var2 = var0.getChild("WhatToBuild").getChild("StrategyType");
      String var3 = var2.getAttributeValue("type");
      if (!var3.equals("template")) {
         return null;
      }

      String var4 = var2.getAttributeValue("templateFile");
      if (var4 == null) {
         throw new Exception("No template file specified!");
      }

      File var5 = new File(var4);
      if (!var5.exists()) {
         var5 = new File(MainApp.getDataPath() + "user/settings/StrategyTemplates/" + var4);
         if (!var5.exists()) {
            throw new StrategyTemplateNotFoundException(var4, var1);
         }

         var4 = MainApp.getDataPath() + "user/settings/StrategyTemplates/" + var4;
      }

      try {
         if (var4.endsWith(".sqw")) {
            return XMLUtil.fileToXmlElement(var5);
         } else if (!var4.endsWith(".sq4") && !var4.endsWith(".sqx")) {
            throw new Exception(String.format("Incorrect template file, only .sqx files can be used as templates!"));
         } else {
            IProgram var7 = Program.get("Loader");
            ResultsGroup var8 = (ResultsGroup)var7.call("loadFile", new Object[]{var4});
            Element var6 = var8.getStrategyXml();
            if (var6 == null) {
               throw new Exception(String.format("Template file doesn't contain strategy!"));
            } else {
               return var6;
            }
         }
      } catch (Exception var9) {
         throw new Exception(String.format("Template file '%s' cannot be loaded. Error: %s!", var4, var9.getMessage()));
      }
   }

   public void setUseSameIdForLongShort(boolean var1) {
      this.useSameIdForLongShort = var1;
   }

   public static void addTickSizeToDataElem(Element var0, String var1, String var2) {
      double var3 = -1.0;
      Object var5 = null;

      try {
         if (!var1.equals("Same as main chart")) {
            var5 = DataManager.getDataInfo("History", var1);
         } else {
            var5 = DataManager.getDataInfo("History", var2);
         }

         if (var5 == null) {
            Log.info("Cannot get data decimals of unknown symbol '" + var1 + "'");
         } else {
            var3 = ((DataInfo)var5).symbolInfo.tickSize;
         }
      } catch (Exception var7) {
         Log.error("Cannot get data decimals of symbol '" + var1 + "'");
      }

      Element var6 = XMLUtil.tryAddElement(var0, "ticksize");
      var6.setText(String.valueOf(var3));
   }
}
