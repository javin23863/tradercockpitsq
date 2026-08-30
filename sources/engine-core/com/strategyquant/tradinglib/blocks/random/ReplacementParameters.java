package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.generator.GenerateException;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementParameters {
   public static final Logger Log = LoggerFactory.getLogger("ReplacementParameters");
   public static final byte Generated = 1;
   public static final byte Predefined = 2;
   public static final byte CDataIndy = 3;
   public byte generationType;
   public int weight;
   private BlockDefinition blockDef;
   public ArrayList<ReplacementParameter> parameters = new ArrayList<>();
   private ReplacementConfig replacementConfig;
   private BlocksConfig blocksConfig;
   public boolean containsFormulas;
   public boolean containsProfitTarget;
   private boolean hasBlockChildren;

   public ReplacementParameters(byte var1, ReplacementsConfig var2, BlocksConfig var3, BlockDefinition var4, ReplacementConfig var5, Element var6, Element var7) throws GenerateException {
      if (var4 == null) {
         throw new GenerateException("Block def cannot be null");
      }

      this.generationType = var1;
      this.blockDef = var4;
      this.replacementConfig = var5;
      this.blocksConfig = var3;

      try {
         this.weight = Integer.parseInt(XMLUtil.getAttr(var6, "weight", "1"));
      } catch (Exception var10) {
         this.weight = 1;
      }

      if (var1 == 1) {
         this.parseGeneratedConfig(var6, var7, var2);
      } else {
         this.parsePredefinedConfig(var6, var7, var2);
      }

      this.containsFormulas = false;
      this.containsProfitTarget = false;
      this.hasBlockChildren = false;

      for (int var8 = 0; var8 < this.parameters.size(); var8++) {
         ReplacementParameter var9 = this.parameters.get(var8);
         if (var9.generation == 3) {
            this.containsFormulas = true;
         }

         if (var9.blockParam.key.contains("ProfitTarget.")) {
            this.containsProfitTarget = true;
         }

         if (var9.blockParam.type.equals("value")) {
            this.hasBlockChildren = true;
         }

         if (this.containsFormulas && this.containsProfitTarget && this.hasBlockChildren) {
            break;
         }
      }
   }

   public ReplacementParameters(
      byte var1, ReplacementsConfig var2, BlocksConfig var3, ReplacementConfig var4, int var5, BlockDefinition var6, int var7, CustomDataInfo var8
   ) throws GenerateException {
      this.generationType = var1;
      this.blockDef = var6;
      this.replacementConfig = var4;
      this.blocksConfig = var3;
      this.containsFormulas = false;
      this.containsProfitTarget = false;
      this.hasBlockChildren = false;
      ReplacementParameter var9 = ReplacementParameter.createCDataIndyShiftParam(var6, var2, var4);
      this.parameters.add(var9);
      var9 = ReplacementParameter.createCDataIndyValueParam(var5, var6, var2, var4, var8);
      this.parameters.add(var9);
   }

   private void parsePredefinedConfig(Element var1, Element var2, ReplacementsConfig var3) throws GenerateException {
      ObjectListIterator var4 = this.blockDef.parameters.iterator();

      while (var4.hasNext()) {
         BlockParameter var5 = (BlockParameter)var4.next();
         ReplacementParameter var6 = new ReplacementParameter((byte)1, var3, var5, this.blocksConfig, this.blockDef, this.replacementConfig);
         List var7 = var1.getChildren();

         for (int var8 = 0; var8 < var7.size(); var8++) {
            Element var9 = (Element)var7.get(var8);
            if (var9.getAttributeValue("key").equals(var5.key)) {
               var6.initFromGeneratedParam(this.blockDef.key, var9, var2);
               break;
            }
         }

         this.parameters.add(var6);
      }
   }

   private void parseGeneratedConfig(Element var1, Element var2, ReplacementsConfig var3) throws GenerateException {
      for (int var4 = 0; var4 < this.blockDef.parameters.size(); var4++) {
         BlockParameter var5 = (BlockParameter)this.blockDef.parameters.get(var4);
         ReplacementParameter var6 = new ReplacementParameter((byte)2, var3, var5, this.blocksConfig, this.blockDef, this.replacementConfig);
         List var7 = var1.getChildren();

         for (int var8 = 0; var8 < var7.size(); var8++) {
            Element var9 = (Element)var7.get(var8);
            if (var9.getAttributeValue("key").equals(var5.key)) {
               var6.initFromGeneratedParam(this.blockDef.key, var9, var2);
               break;
            }
         }

         this.parameters.add(var6);
      }
   }

   public void fixParametersOrder(Element var1) throws GenerateException {
      HashMap var2 = new HashMap();

      for (Element var4 : var1.getChildren()) {
         var2.put(var4.getAttributeValue("key"), var4);
      }

      for (ReplacementParameter var7 : this.parameters) {
         Element var5 = (Element)var2.get(var7.blockParam.key);
         if (var5 == null) {
            throw new GenerateException("Block parameters map doesn't contain parameter with key '" + var7.blockParam.key + "'");
         }

         var5.detach();
         var1.addContent(var5);
      }
   }

   public boolean hasBlockChildren() {
      return this.hasBlockChildren;
   }

   public ReplacementParameter getChartParam() {
      for (int var1 = 0; var1 < this.parameters.size(); var1++) {
         ReplacementParameter var2 = this.parameters.get(var1);
         if (var2.blockParam.type.equals("data")) {
            return var2;
         }
      }

      return null;
   }

   public ReplacementParameter findReplacementParamByKey(String var1) {
      for (int var2 = 0; var2 < this.parameters.size(); var2++) {
         ReplacementParameter var3 = this.parameters.get(var2);
         if (var3.blockParam.key.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public boolean verifyItem(Element var1) throws GenerateException {
      List var2 = var1.getChildren("Param");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         String var6 = var4.getAttributeValue("randomValue");
         if (var6 != null && (var6.equals("") || var6.equals("default"))) {
            var6 = null;
         }

         if (var6 != null) {
            if (!this.verifyRandomValue(var4, var6)) {
               return false;
            }
         } else if (!this.verifyParamConfig(var4)) {
            return false;
         }
      }

      return true;
   }

   private boolean verifyParamConfig(Element var1) throws GenerateException {
      String var2 = var1.getAttributeValue("key");
      ReplacementParameter var3 = this.findReplacementParamByKey(var2);
      if (var3 == null) {
         Log.info("ReplacementParam for '" + var2 + "' not found!");
         return true;
      } else {
         return var3.verifyParam(var2, var1);
      }
   }

   private boolean verifyRandomValue(Element var1, String var2) {
      String var3 = var1.getAttributeValue("type");
      boolean var4 = var3.equals("double");

      try {
         RandomValueConfig var5 = RandomValueConfig.parseRandomValue(var2, var4);
         if (var5 == null) {
            Log.error("Error parsing randomValue '" + var2 + "' in strategy template.");
            return true;
         }

         String var6 = var1.getText();
         if (var6 != null && !var6.isBlank()) {
            double var7;
            if (var4) {
               var7 = Double.parseDouble(var6);
            } else {
               var7 = Integer.parseInt(var6);
            }

            return var5.isInValidRange(var7);
         }
      } catch (Exception var9) {
         Log.error("Exception ", var9);
      }

      return true;
   }
}
