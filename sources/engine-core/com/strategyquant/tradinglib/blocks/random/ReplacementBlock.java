package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.generator.BlockSettingsConverter;
import com.strategyquant.tradinglib.generator.GenerateException;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementBlock {
   public static final Logger Log = LoggerFactory.getLogger("ReplacementBlock");
   public int weight;
   public String key;
   public ReplacementParameterGroups parameters = new ReplacementParameterGroups();
   public BlockDefinition blockDef;
   private ReplacementConfig replacementConfig;
   private boolean hasBlockChildren;
   public double indicatorMin;
   public double indicatorMax;
   public double indicatorStep;
   private String cdataId;
   private int cdataType;
   private Element xml;

   public ReplacementBlock(ReplacementsConfig var1, String var2, BlocksConfig var3, Element var4, ReplacementConfig var5) throws GenerateException {
      this.replacementConfig = var5;
      this.key = var2;
      if (this.key.startsWith("CDataIndy")) {
         this.initCustomDataIndy(var4, var1, var3);
      } else {
         this.weight = Integer.parseInt(var4.getAttributeValue("weight"));
         this.blockDef = var3.getBlock(var2);
         if (this.blockDef == null) {
            throw new GenerateException(String.format("Block '%s' cannot be found!", var2));
         }

         String var6 = var4.getAttributeValue("keepParams");
         if (var6 != null && var6.equals("true")) {
            this.xml = this.createCorrectBlockElement(var4, this.blockDef.getXml());
         } else {
            this.xml = this.blockDef.getXml();
         }

         try {
            this.indicatorMin = this.loadDoubleAttribute(var4, "indicatorMin", this.blockDef.indicatorMin);
            this.indicatorMax = this.loadDoubleAttribute(var4, "indicatorMax", this.blockDef.indicatorMax);
            this.indicatorStep = this.loadDoubleAttribute(var4, "indicatorStep", this.blockDef.indicatorStep);
         } catch (NumberFormatException var12) {
            String var8 = XMLUtil.elementToString(var4);
            throw new NumberFormatException("Problem parsing double, XML: " + var8);
         }

         if (var4.getChild("Generated") != null) {
            ReplacementParameters var7 = new ReplacementParameters(
               (byte)1, var1, var3, this.blockDef, var5, var4.getChild("Generated"), var4.getChild("Formulas")
            );
            this.hasBlockChildren = var7.hasBlockChildren();
            this.parameters.add(var7);
         }

         if (var4.getChild("Predefined") != null) {
            List var13 = var4.getChild("Predefined").getChildren();

            for (int var9 = 0; var9 < var13.size(); var9++) {
               Element var10 = (Element)var13.get(var9);
               ReplacementParameters var11 = new ReplacementParameters((byte)2, var1, var3, this.blockDef, var5, var10, null);
               this.parameters.add(var11);
            }
         }
      }
   }

   private Element createCorrectBlockElement(Element var1, Element var2) throws GenerateException {
      Element var3 = new Element("Item");
      XMLUtil.copyAttributes(var1, var3, false);
      XMLUtil.copyAttributes(var2, var3, false);
      Element var4 = var1.getChild("Generated");
      if (var4 == null) {
         throw new GenerateException("Incorrect block format for random group!");
      }

      List var5 = var4.getChildren();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         var3.addContent(var7.clone());
      }

      return var3;
   }

   private void initCustomDataIndy(Element var1, ReplacementsConfig var2, BlocksConfig var3) throws GenerateException {
      this.weight = Integer.parseInt(var1.getAttributeValue("weight"));
      this.cdataId = var1.getAttributeValue("id");
      this.cdataType = Integer.parseInt(var1.getAttributeValue("dataType"));
      CustomDataInfo var4 = CustomDataManager.getDataInfo(this.cdataId);
      if (var4 == null) {
         throw new GenerateException("Cannot find custom data indicator '" + this.cdataId + "', ignoring!");
      }

      this.blockDef = this.createBlockDefForCDataIndy(var4);
      this.xml = this.blockDef.getXml();
      ReplacementParameters var5 = new ReplacementParameters(
         (byte)3, var2, var3, this.replacementConfig, var4.values, this.blockDef, Integer.parseInt(var1.getAttributeValue("weight")), var4
      );
      this.hasBlockChildren = false;
      this.parameters.add(var5);
   }

   private BlockDefinition createBlockDefForCDataIndy(CustomDataInfo var1) throws GenerateException {
      BlockDefinition var2 = new BlockDefinition(this.cdataId, this.cdataType);
      var2.key = "CDataIndy_" + this.cdataId;
      var2.name = this.cdataId;
      Element var3 = BlockSettingsConverter.createCDataShiftParamElement();
      var2.parameters.add(new BlockParameter(var3));
      var3 = BlockSettingsConverter.createCDataValueParamElement(var1.values, var1);
      var2.parameters.add(new BlockParameter(var3));
      switch (this.cdataType) {
         case 1:
            var2.superType = 4;
            var2.returnType = 2;
            break;
         case 2:
            var2.superType = 4;
            var2.returnType = 1;
            break;
         case 3:
            var2.superType = 4;
            var2.returnType = 7;
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         default:
            break;
         case 10:
            var2.superType = 3;
            var2.returnType = 3;
            break;
         case 11:
            var2.superType = 3;
            var2.returnType = 3;
      }

      return var2;
   }

   private double loadDoubleAttribute(Element var1, String var2, double var3) {
      String var5 = var1.getAttributeValue(var2);
      return var5 != null && !var5.equals("undefined") ? Double.parseDouble(var5) : var3;
   }

   public boolean hasBlockChildren() {
      return this.hasBlockChildren;
   }

   public Element getXml() {
      return this.xml;
   }

   public ReplacementParameter findReplacementParamByKey(String var1) {
      for (int var2 = 0; var2 < this.parameters.size(); var2++) {
         ReplacementParameters var3 = this.parameters.get(var2);
         ReplacementParameter var4 = var3.findReplacementParamByKey(var1);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   public boolean verifyItem(Element var1) throws GenerateException {
      for (int var2 = 0; var2 < this.parameters.size(); var2++) {
         ReplacementParameters var3 = this.parameters.get(var2);
         if (var3.verifyItem(var1)) {
            return true;
         }
      }

      return false;
   }
}
