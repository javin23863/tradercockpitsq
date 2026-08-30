package com.strategyquant.tradinglib.task.settings.blocks;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlockParameter;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.generator.BlockSettingsConverter;
import com.strategyquant.tradinglib.generator.StrategyTemplateGenerator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigConverter {
   public static final Logger Log = LoggerFactory.getLogger("ConfigConverter");
   private String taskName;

   public ConfigConverter(String var1) {
      this.taskName = var1;
   }

   public Element convertSimpleBuildingBlocks(Element var1, IRandomGenerator var2) throws JDOMException, IOException, Exception {
      Element var3 = new Element("Settings");
      Element var4 = null;

      for (Element var6 : var1.getChildren()) {
         if (!var6.getName().equals("Blocks")) {
            var3.addContent(var6.clone());
         } else {
            var4 = var6;
         }
      }

      if (var4 == null) {
         throw new Exception(L.t("Builder project config doesn't contain Blocks element.", new Object[0]));
      }

      var1 = var1.clone();
      XMLUtil.xmlToFile(var1, new File(MainApp.getDataPath() + "tests/tmp/simpleBuilderConfig.xml"));
      Element var8 = this.getCorrectStrategyTemplate(var1, var2);
      var3.addContent(var8);
      XMLUtil.xmlToFile(var8, new File(MainApp.getDataPath() + "tests/tmp/StrTemplateBefore.xml"));
      var3.addContent(this.convertToFullFormat(var1, var8));
      this.deleteRandomGroupsElement(var3);
      XMLUtil.xmlToFile(var3, new File(MainApp.getDataPath() + "tests/tmp/fullBuilderConfig.xml"));
      return var3;
   }

   private void deleteRandomGroupsElement(Element var1) {
      Element var2 = var1.getChild("StrategyFile").getChild("Strategy");
      var2.removeChild("RandomGroups");
   }

   private Element getCorrectStrategyTemplate(Element var1, IRandomGenerator var2) throws Exception {
      StrategyTemplateGenerator var3 = new StrategyTemplateGenerator(this.taskName, var1, var2);
      return var3.generate().detach();
   }

   private Element convertToFullFormat(Element var1, Element var2) throws Exception {
      BlockSettingsConverter var3 = new BlockSettingsConverter(var1.clone(), var2);
      Element var4 = var3.convert();
      this.fixIchimokuChikouSpan(var4);
      BlocksConfig var5 = BlocksConfig.getConfig();
      this.fixSPParamIssues(var4, var5);
      return var4;
   }

   private void fixSPParamIssues(Element var1, BlocksConfig var2) {
      List var3 = var1.getChildren();
      if (var3 != null && var3.size() > 0) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            this.fixSPParamIssues(var5, var2);
         }
      }

      if (var1.getName().equals("Block")) {
         String var6 = var1.getAttributeValue("key");
         if (var6 == null || !var6.startsWith("talib_")) {
            return;
         }

         BlockDefinition var7 = var2.getBlock(var6);
         if (var7 == null) {
            return;
         }

         this.fixSPParamIssuesOnItemParams(var1, var7);
      }
   }

   private void fixSPParamIssuesOnItemParams(Element var1, BlockDefinition var2) {
      List var3 = var1.getChildren();
      if (var3 != null && var3.size() > 0) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            this.fixSPParamIssuesOnItemParams(var5, var2);
         }
      }

      if (var1.getName().equals("Param")) {
         Element var10 = var1;
         String var11 = var10.getAttributeValue("key");
         if (var11 == null) {
            return;
         }

         BlockParameter var6 = var2.getParamByKey(var11);
         if (var6 == null) {
            return;
         }

         if (var6.paramType != null && var6.paramType.equals("period")) {
            String var7 = var10.getAttributeValue("minValue");
            if (var7 == null) {
               return;
            }

            double var8 = Double.parseDouble(var7);
            if (var8 >= 0.0 && var8 < 2.0) {
               if (Log.isDebugEnabled()) {
                  Log.debug("Corrected min period value from {} to 2, param: {}", var7, XMLUtil.elementToString(var10));
               }

               var10.setAttribute("minValue", "2");
            }
         }
      }
   }

   private void fixIchimokuChikouSpan(Element var1) {
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         if (var4.getName().equals("Param")) {
            String var5 = var4.getAttributeValue("values");
            if (var5 != null && var5.contains(",ChikouSpan=4")) {
               var5 = var5.replace(",ChikouSpan=4", "");
               var4.setAttribute("values", var5);
               continue;
            }
         }

         this.fixIchimokuChikouSpan(var4);
      }
   }
}
