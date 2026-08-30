package com.strategyquant.tradinglib.strategy.xml;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlockParameter;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;

public class SQ3StrategyConverter {
   private static SQ3StrategyConverter instance;
   private Transformer transformer;
   private BlocksConfig blocksConfig = BlocksConfig.getConfig();
   private Element defaultSize = new Element("Formula");
   private Element defaultRangeLevel;
   private Element defaultRange;
   private Element defaultSLPT;
   private int existingRules;

   public static synchronized SQ3StrategyConverter getInstance() throws Exception {
      if (instance == null) {
         instance = new SQ3StrategyConverter();
      }

      return instance;
   }

   public SQ3StrategyConverter() throws JDOMException, IOException, Exception {
      this.defaultSize.setAttribute("key", "SQ.Formulas.Size.UseGlobalMM");
      this.defaultRangeLevel = new Element("Formula");
      this.defaultRangeLevel.setAttribute("key", "SQ.Formulas.RangeLevel.None");
      this.defaultRange = new Element("Formula");
      this.defaultRange.setAttribute("key", "SQ.Formulas.Range.None");
      this.defaultSLPT = new Element("Formula");
      this.defaultSLPT.setAttribute("key", "SQ.Formulas.SLPT.None");
   }

   public Element convert(Element var1) throws Exception {
      String var2 = "transformStrategyQuant.xslt";

      try {
         if (var1.getChild("Strategy").getChildren("TradeRule").size() > 0) {
            var2 = "transformEA.xslt";
         }
      } catch (Exception var11) {
      }

      StreamSource var3 = new StreamSource(new File(MainApp.getDataPath() + "internal/web/SQWIZARD/xslt/" + var2));
      TransformerFactory var4 = TransformerFactory.newInstance();
      this.transformer = var4.newTransformer(var3);
      String var5 = XMLUtil.elementToString(var1);
      StreamSource var6 = new StreamSource(new StringReader(var5));
      StringWriter var7 = new StringWriter();
      StreamResult var8 = new StreamResult(var7);
      this.transformer.transform(var6, var8);
      String var9 = var7.toString();
      var9 = this.normalizeXml(var9);
      var9 = SQUtils.fixRenamedActionParams(var9);
      Element var10 = XMLUtil.stringToElement(var9);
      this.existingRules = 0;
      var10 = this.fixStrategy(var10);
      this.removeUnusedMagicVariables(var10);
      this.removeEmptyBlocks(var10);
      return var10;
   }

   private void removeUnusedMagicVariables(Element var1) {
      String var2 = XMLUtil.elementToString(var1);
      this.removeUnusedVariable("MagicNumber", var2, var1);
      this.removeUnusedVariable("MagicLong", var2, var1);
      this.removeUnusedVariable("MagicShort", var2, var1);
   }

   private void removeUnusedVariable(String var1, String var2, Element var3) {
      int var4 = 0;
      boolean var5 = false;

      while (true) {
         int var6 = var2.indexOf(var1, var4);
         if (var6 == -1) {
            break;
         }

         String var7 = var2.substring(var6 + var1.length(), var6 + var1.length() + 5);
         if (!var7.equals("</id>") && !var7.equals("</nam")) {
            var5 = true;
            break;
         }

         var4 = var6 + 1;
      }

      if (!var5) {
         Element var12 = var3.getChild("Strategy").getChild("Variables");
         List var13 = var12.getChildren();

         for (int var8 = 0; var8 < var13.size(); var8++) {
            Element var9 = (Element)var13.get(var8);
            Element var10 = var9.getChild("id");
            String var11 = var10.getText();
            if (var11.equals(var1)) {
               var12.removeContent(var9);
               break;
            }
         }
      }
   }

   private Element fixStrategy(Element var1) throws Exception {
      Element var2 = this.copyElement(var1);
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         Content var6 = null;
         String var7 = var5.getName();
         switch (var7) {
            case "Item":
               var6 = this.fixItem(var5);
               break;
            case "Rule":
               var6 = this.fixRule(var5);
               break;
            default:
               var6 = this.fixStrategy(var5);
         }

         var2.addContent(var6);
      }

      if (var3.size() == 0) {
         String var10 = var1.getText();
         if (var10 != null) {
            var2.setText(var10);
         }
      }

      return var2;
   }

   private Element fixRule(Element var1) throws Exception {
      Element var2 = this.fixStrategy(var1);
      String var3 = null;
      switch (this.existingRules) {
         case 0:
            var3 = "Long Entry";
            break;
         case 1:
            var3 = "Short Entry";
            break;
         case 2:
            var3 = "Long Exit";
            break;
         case 3:
            var3 = "Short Exit";
      }

      this.existingRules++;
      if (var3 == null) {
         throw new Exception("Rule name not recognized!");
      }

      var2.setAttribute("name", var3);
      return var2;
   }

   private Element copyElement(Element var1) {
      Element var2 = new Element(var1.getName());
      List var3 = var1.getAttributes();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         var2.setAttribute(((Attribute)var3.get(var4)).getName(), ((Attribute)var3.get(var4)).getValue());
      }

      return var2;
   }

   private Element fixItem(Element var1) throws Exception {
      Element var2 = this.copyElement(var1);
      var2 = this.fixItemAttributes(var2);
      String var3 = var1.getAttributeValue("key");
      BlockDefinition var4 = this.blocksConfig.getBlock(var3);
      if (!var3.equals("AND") && !var3.equals("OR")) {
         for (int var5 = 0; var5 < var4.parameters.size(); var5++) {
            BlockParameter var6 = (BlockParameter)var4.parameters.get(var5);
            Element var7 = this.findParamByKey(var1, var6.key);
            Element var8 = null;
            if (var7 == null) {
               var8 = var6.getParamElement();
               String var9 = var8.getAttributeValue("isFormula");
               if (var9 != null && !var9.equals("false")) {
                  String var18 = var8.getAttributeValue("key");
                  switch (var18) {
                     case "#Size#":
                        var8.addContent(this.defaultSize.clone());
                        break;
                     case "#MoveSL2BE.MoveSL2BE#":
                        var8.addContent(this.defaultRangeLevel.clone());
                        break;
                     case "#MoveSL2BE.SL2BEAddPips#":
                        var8.addContent(this.defaultRange.clone());
                        break;
                     case "#TrailingStop.TrailingStop#":
                        var8.addContent(this.defaultRangeLevel.clone());
                        break;
                     case "#TrailingStop.TrailingActivation#":
                        var8.addContent(this.defaultRange.clone());
                        break;
                     case "#StopLoss.StopLoss#":
                        var8.addContent(this.defaultSLPT.clone());
                        break;
                     case "#ProfitTarget.ProfitTarget#":
                        var8.addContent(this.defaultSLPT.clone());
                        break;
                     default:
                        throw new Exception("Unknown item " + var3);
                  }
               } else {
                  String var10 = var8.getAttributeValue("defaultValue");
                  if (var10 == null) {
                     throw new Exception("Element doesn't have default value!");
                  }

                  String var11 = var8.getAttributeValue("key");
                  if (!var11.equals("#ReplaceExisting#") || !XMLUtil.elementIs(var8, "variable")) {
                     var8.setText(var10);
                  }

                  if (var11.equals("#MagicNumber#")) {
                     var8.setAttribute("variable", "true");
                     var8.setAttribute("variableType", "INT");
                  }

                  if (var11.equals("#Direction#") && (this.existingRules == 1 || this.existingRules == 3)) {
                     var8.setText("-1");
                  }
               }
            } else {
               var8 = var6.getParamElement();
               if (var7.getChildren().size() > 1) {
                  throw new Exception("Has more than 1 child");
               }

               if (var7.getChildren().size() > 0) {
                  Element var17 = (Element)var7.getChildren().get(0);
                  Element var19 = null;
                  String var21 = var17.getName();
                  switch (var21) {
                     case "Item":
                        var19 = this.fixItem(var17);
                        break;
                     case "Formula":
                        var19 = this.fixStrategy(var17);
                  }

                  if (var19 == null) {
                     throw new Exception("Unknown child element: " + var21);
                  }

                  var8.addContent(var19);
               } else {
                  String var16 = var8.getAttributeValue("key");
                  if (var16.equals("#MagicNumber#")) {
                     var8.setAttribute("variable", "true");
                     var8.setAttribute("variableType", "INT");
                  } else if (var16.equals("#ReplaceExisting#") && XMLUtil.elementIs(var7, "variable")) {
                     var8.setAttribute("variable", "true");
                     var8.setAttribute("variableType", "BOOL");
                  } else if (var16.equals("#Variable#") && XMLUtil.elementIs(var7, "variable")) {
                     var8.setAttribute("variable", "true");
                  }

                  var8.setText(var7.getText());
               }

               var8.setName(var7.getName());
            }

            var2.addContent(var8);
         }

         return var2;
      } else {
         return this.processAndOr(var2, var1, this.existingRules);
      }
   }

   private Element processAndOr(Element var1, Element var2, int var3) throws Exception {
      List var4 = var2.getChildren();

      for (int var6 = 0; var6 < var4.size(); var6++) {
         Element var7 = (Element)var4.get(var6);
         String var8 = var7.getName();
         Element var5;
         switch (var8) {
            case "Item":
               var5 = this.fixItem(var7);
               break;
            default:
               var5 = this.fixStrategy(var7);
         }

         var1.addContent(var5);
      }

      return var1;
   }

   private Element fixItemAttributes(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("key");
      if (!var2.equals("AND") && !var2.equals("OR")) {
         BlockDefinition var3 = this.blocksConfig.getBlock(var2);
         if (var3 == null) {
            throw new Exception("Key " + var2 + " wasn't found in blocks");
         }

         Element var4 = var3.getXml();
         List var5 = var4.getAttributes();

         for (int var6 = 0; var6 < var5.size(); var6++) {
            String var7 = var1.getAttributeValue(((Attribute)var5.get(var6)).getName());
            if (var7 == null) {
               var1.setAttribute(((Attribute)var5.get(var6)).getName(), ((Attribute)var5.get(var6)).getValue());
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   private Element findParamByKey(Element var1, String var2) {
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         String var5 = ((Element)var3.get(var4)).getAttributeValue("key");
         if (var5 != null && var5.equals(var2)) {
            return (Element)var3.get(var4);
         }
      }

      return null;
   }

   private String normalizeXml(String var1) {
      return var1.replace("<ParamX", "<Param")
         .replace("</ParamX", "</Param")
         .replace("xmlns:wizard=\"http://example.com/wizard\"", "")
         .replace("\n", "")
         .replace("\r", "")
         .replace("\t", "")
         .replaceAll(" +", " ")
         .replace("> <", "><")
         .replace(" >", ">");
   }

   private void removeEmptyBlocks(Element var1) {
      List var2 = var1.getChildren();

      for (int var3 = var2.size() - 1; var3 >= 0; var3--) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getName();
         switch (var5) {
            case "Item":
            case "Block":
               String var8 = var4.getAttributeValue("key");
               if (var8 == null || !var8.equals("AlwaysTrue")) {
                  if (var4.getChildren().size() == 0) {
                     var1.removeContent(var4);
                  } else {
                     this.removeEmptyBlocks(var4);
                  }
               }
               break;
            default:
               if (var4.getChildren().size() > 0) {
                  this.removeEmptyBlocks(var4);
               }
         }
      }
   }
}
