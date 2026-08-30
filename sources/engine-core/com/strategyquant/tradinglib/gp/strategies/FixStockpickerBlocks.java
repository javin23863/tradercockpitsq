package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlockParameter;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixStockpickerBlocks implements IEvolutionaryOperator<Node> {
   private static final Logger Log = LoggerFactory.getLogger("FixStockpickerBlocks");
   private BlocksConfig _blocksConfig;

   public FixStockpickerBlocks() throws GenerateException {
      try {
         this._blocksConfig = BlocksConfig.getConfig();
      } catch (Exception var2) {
         throw new GenerateException("Cannot initialize blocks config!", var2);
      }
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      for (int var6 = 0; var6 < var1.size(); var6++) {
         Node var7 = (Node)var1.get(var6);
         fixSPStrategy(var7.getStrategy(), this._blocksConfig);
      }

      return var1;
   }

   public static void fixSPStrategy(Element var0, BlocksConfig var1) throws GenerateException {
      Element var2 = var0.getChild("Strategy");
      String var3 = var2.getAttributeValue("engine");
      if (var3 == null || var3.equals("Stockpicker") || var3.equals("Single-asset cloud strategy")) {
         fixSPParamIssues(var0, var1);
      }
   }

   private static void fixSPParamIssues(Element var0, BlocksConfig var1) {
      if (var0.getName().equals("Item")) {
         fixSPParamIssuesOnItem(var0, var1);
      }

      List var2 = var0.getChildren();
      if (var2 != null && var2.size() > 0) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            fixSPParamIssues(var4, var1);
         }
      }
   }

   private static void fixSPParamIssuesOnItem(Element var0, BlocksConfig var1) {
      String var2 = var0.getAttributeValue("key");
      if (var2 != null) {
         if (var2.startsWith("talib_")) {
            fixTALibParams(var2, var0, var1);
         } else {
            fixDWMBlockShift(var2, var0);
         }
      }
   }

   private static void fixDWMBlockShift(String var0, Element var1) {
      if (var0.endsWith("D")
         || var0.endsWith("W")
         || var0.endsWith("M")
         || var0.startsWith("Open")
         || var0.endsWith("High")
         || var0.endsWith("Low")
         || var0.endsWith("Close")) {
         if (var0.endsWith("Open") || var0.endsWith("High") || var0.endsWith("Low") || var0.endsWith("Close")) {
            String var2 = var1.getAttributeValue("generated");
            if (var2 == null || var2.equals("none")) {
               return;
            }
         }

         List var7 = var1.getChildren("Param");

         for (int var3 = 0; var3 < var7.size(); var3++) {
            Element var4 = (Element)var7.get(var3);
            String var5 = var4.getAttributeValue("key");
            if (var5 != null && var5.equals("#Shift#")) {
               String var6 = var4.getText();
               if (var6 != null && var6.equals("0")) {
                  var4.setText("1");
                  if (Log.isDebugEnabled()) {
                     Log.debug("Corrected Shift value 0->1, for {}", var6, var0);
                  }
               }
            }
         }
      }
   }

   private static void fixTALibParams(String var0, Element var1, BlocksConfig var2) {
      BlockDefinition var3 = var2.getBlock(var0);
      if (var3 != null) {
         List var4 = var1.getChildren("Param");

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Element var6 = (Element)var4.get(var5);
            String var7 = var6.getAttributeValue("key");
            if (var7 != null) {
               BlockParameter var8 = var3.getParamByKey(var7);
               if (var8 != null && !isCustomParamReference(var6)) {
                  if (var8.paramType != null) {
                     if (var8.paramType.equals("period")) {
                        String var16 = var6.getText();
                        if (var16 != null) {
                           double var18 = Double.parseDouble(var16);
                           if (var18 < 2.0) {
                              if (Log.isDebugEnabled()) {
                                 Log.debug("Corrected period value from {} to 2, param: {}", var16, XMLUtil.elementToString(var6));
                              }

                              var6.setText("2");
                           }
                        }
                     } else if (var8.paramType.equals("shift")) {
                        String var17 = var6.getText();
                        if (var17 != null) {
                           double var19 = Double.parseDouble(var17);
                           if (var19 < 0.0) {
                              if (Log.isDebugEnabled()) {
                                 Log.debug("Corrected shift value from {} to 1 (min), param: {}", var17, XMLUtil.elementToString(var6));
                              }

                              var6.setText("1");
                           }
                        }
                     }
                  } else if (var8.controlType != null && var8.controlType.equals("combo") && var8.values != null) {
                     String var9 = var6.getText();
                     if (var9 != null) {
                        String[] var10 = var8.values.split(",");
                        boolean var11 = false;
                        String var12 = null;

                        for (int var13 = 0; var13 < var10.length; var13++) {
                           String[] var14 = var10[var13].split("=");
                           String var15 = var14[1];
                           if (var13 == 0) {
                              var12 = var15;
                           }

                           if (var9.equals(var15)) {
                              var11 = true;
                              break;
                           }
                        }

                        if (!var11) {
                           if (Log.isDebugEnabled()) {
                              Log.debug("Corrected value outside range from {}, param: {}", var9, XMLUtil.elementToString(var6));
                           }

                           if (var8.defaultValue != null) {
                              var6.setText(var8.defaultValue);
                           } else {
                              var6.setText(var12);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isCustomParamReference(Element var0) {
      if ("true".equals(var0.getAttributeValue("customParam"))) {
         return true;
      }

      String var1 = var0.getText();
      return var1 != null && var1.length() >= 2 && var1.startsWith("#") && var1.endsWith("#");
   }
}
