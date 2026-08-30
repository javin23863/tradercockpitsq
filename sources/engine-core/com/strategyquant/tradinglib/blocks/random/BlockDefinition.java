package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.tradinglib.BlockSuperTypes;
import com.strategyquant.tradinglib.ReturnTypes;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.Serializable;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockDefinition implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("BlockDefinition");
   public String key;
   public String name;
   public int superType;
   public ObjectArrayList<BlockParameter> parameters = new ObjectArrayList();
   private Element elBlock;
   public boolean notFirstValue = false;
   public boolean isIndicator = false;
   public double indicatorMin;
   public double indicatorMax;
   public double indicatorStep;
   public int returnType;
   public String categoryType = null;

   public BlockDefinition(Element var1, Element var2, String var3) throws Exception {
      this.key = var1.getAttributeValue("key");
      this.name = var1.getAttributeValue("name");
      this.superType = BlockSuperTypes.translate(var3);
      this.returnType = ReturnTypes.translate(var1.getAttributeValue("returnType"));
      this.categoryType = var1.getAttributeValue("categoryType");
      this.elBlock = var1;
      String var4 = var1.getAttributeValue("notFirstValue");
      if (var4 != null && var4.equals("true")) {
         this.notFirstValue = true;
      }

      if (var1.getAttributeValue("categoryType") != null) {
         this.isIndicator = var1.getAttributeValue("categoryType").equals("indicator");
      }

      String var5 = var1.getAttributeValue("indicatorMin");
      String var6 = var1.getAttributeValue("indicatorMax");
      String var7 = var1.getAttributeValue("indicatorStep");
      this.indicatorMin = var5 != null ? Double.parseDouble(var5) : -999999.0;
      this.indicatorMax = var6 != null ? Double.parseDouble(var6) : 999999.0;
      this.indicatorStep = var7 != null ? Double.parseDouble(var7) : 0.0;
      this.parseParameters(var2);
   }

   BlockDefinition(String var1, int var2) {
      this.elBlock = new Element("Item");
      this.elBlock.setAttribute("key", "CDataIndy_" + var1);
      this.elBlock.setAttribute("cdataType", Integer.toString(var2));
   }

   private void parseParameters(Element var1) {
      if (var1 != null) {
         for (Element var3 : var1.getChildren()) {
            for (Element var5 : var3.getChildren("Param")) {
               this.parameters.add(new BlockParameter(var5));
            }
         }
      }

      if (this.superType == 2) {
         int var6 = this.computeValueParameters();
         if (var6 == 1) {
            this.superType = 1;
         }
      }
   }

   private int computeValueParameters() {
      int var1 = 0;
      ObjectListIterator var2 = this.parameters.iterator();

      while (var2.hasNext()) {
         BlockParameter var3 = (BlockParameter)var2.next();
         if (var3.isValueParameter()) {
            var1++;
         }
      }

      return var1;
   }

   public Element getXml() {
      return this.elBlock;
   }

   public String getKey() {
      return this.key;
   }

   public String getName() {
      return this.name;
   }

   public BlockParameter getParamByKey(String var1) {
      for (int var2 = 0; var2 < this.parameters.size(); var2++) {
         BlockParameter var3 = (BlockParameter)this.parameters.get(var2);
         if (var3.key.equals(var1)) {
            return var3;
         }
      }

      return null;
   }
}
