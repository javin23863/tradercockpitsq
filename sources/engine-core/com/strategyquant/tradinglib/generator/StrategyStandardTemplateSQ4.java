package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyStandardTemplateSQ4 extends StrategyStandardTemplateSQ3 {
   public static final Logger Log = LoggerFactory.getLogger("StrategyStandardTemplateSQ4");
   public static final String SGN_LONG_ENTRY_ID = "33333333-1111-1111-3333-333333333333";
   public static final String SGN_LONG_ENTRY_NAME = "LongEntrySignal";
   public static final String SGN_LONG_EXIT_ID = "33333333-1111-2222-3333-333333333333";
   public static final String SGN_LONG_EXIT_NAME = "LongExitSignal";
   public static final String SGN_SHORT_ENTRY_ID = "33333333-2222-1111-3333-333333333333";
   public static final String SGN_SHORT_ENTRY_NAME = "ShortEntrySignal";
   public static final String SGN_SHORT_EXIT_ID = "33333333-2222-2222-3333-333333333333";
   public static final String SGN_SHORT_EXIT_NAME = "ShortExitSignal";

   public StrategyStandardTemplateSQ4(Element var1, IRandomGenerator var2) {
      super(var1, var2);
   }

   @Override
   public Element generate() throws Exception {
      Element var1 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/template.xml"));
      Element var2 = (Element)var1.getChild("Strategy").getChild("Rules").getChild("Events").getChildren("Event").get(1);
      this.addRules(var2);
      this.addDatas(var1);
      this.addVariables(var1);
      return var1;
   }

   @Override
   protected void addVariables(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("Variables");
      if (this.useSameIdForLongShort) {
         this.createVariable(var2, "11111111-1111-1111-1111-111111111111", "MagicNumber", 11111);
      } else {
         this.createVariable(var2, "11111111-1111-1111-1111-111111111111", "MagicLong", 11111);
         this.createVariable(var2, "22222222-2222-2222-2222-222222222222", "MagicShort", 22222);
      }

      this.createBoolVariable(var2, "33333333-1111-1111-3333-333333333333", "LongEntrySignal", false);
      this.createBoolVariable(var2, "33333333-2222-1111-3333-333333333333", "ShortEntrySignal", false);
      if (this.containsExitRule) {
         this.createBoolVariable(var2, "33333333-1111-2222-3333-333333333333", "LongExitSignal", false);
         this.createBoolVariable(var2, "33333333-2222-2222-3333-333333333333", "ShortExitSignal", false);
      }
   }

   private void createBoolVariable(Element var1, String var2, String var3, boolean var4) {
      Element var5 = new Element("variable");
      var5.addContent(new Element("id").addContent(var2));
      var5.addContent(new Element("name").addContent(var3));
      var5.addContent(new Element("type").addContent("boolean"));
      var5.addContent(new Element("value").addContent(Boolean.toString(var4)));
      var5.addContent(new Element("makeExternal").addContent("false"));
      var1.addContent(var5);
   }

   private void addRules(Element var1) throws Exception {
      Element var2 = this.getSignalRule();
      var1.addContent(var2);
      if (this.containsExitRule) {
         this.addEntryRule(var1, "Long entry", "longentryrule.xml", 1);
         this.addEntryRule(var1, "Short entry", "shortentryrule.xml", -1);
         this.addExitRule(var1, "Long exit", "longexitrule.xml", 1);
         this.addExitRule(var1, "Short exit", "shortexitrule.xml", -1);
      } else {
         this.addEntryRule(var1, "Long entry", "longentryrulenoexit.xml", 1);
         this.addEntryRule(var1, "Short entry", "shortentryrulenoexit.xml", -1);
      }
   }

   private void addEntryRule(Element var1, String var2, String var3, int var4) throws JDOMException, IOException, Exception {
      Element var5 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/" + var3)).detach();
      var5.setAttribute("name", var2);
      var1.addContent(var5);
      if (var4 == 1) {
         if (this.marketSides.equals("both") || this.marketSides.equals("long")) {
            Element var6 = this.createRandomAction(var5.getChild("Then").getChild("Item"), "RandomActionLong", 1, null, false, false);
            var5.getChild("Then").addContent(var6.detach());
         }
      } else if (this.marketSides.equals("both") || this.marketSides.equals("short")) {
         Element var7 = this.createRandomAction(
            var5.getChild("Then").getChild("Item"), "RandomActionShort", -1, "RandomActionLong", this.entrySymmetry, this.exitSymmetry
         );
         var5.getChild("Then").addContent(var7.detach());
      }
   }

   private void addExitRule(Element var1, String var2, String var3, int var4) throws JDOMException, IOException, Exception {
      Element var5 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/" + var3)).detach();
      var5.setAttribute("name", var2);
      var1.addContent(var5);
      if (var4 == -1) {
         try {
            Element var6 = XMLUtil.getItemParameter(var5.getChild("Then").getChild("Item"), "#Direction#", false);
            if (var6 != null) {
               var6.setText(Integer.toString(var4));
            }
         } catch (Exception var7) {
         }

         if (!this.useSameIdForLongShort) {
            Element var8 = (Element)((Element)var5.getChild("If").getChild("Item").getChildren("Block").get(1)).getChild("Item").getChildren("Param").get(1);
            var8.setText("22222222-2222-2222-2222-222222222222");
            var8 = (Element)var5.getChild("Then").getChild("Item").getChildren("Param").get(2);
            var8.setText("22222222-2222-2222-2222-222222222222");
         }
      }
   }

   protected Element getSignalRule() throws JDOMException, IOException, Exception {
      Element var1 = new Element("Rule");
      var1.setAttribute("name", "Trading signals");
      var1.setAttribute("type", "Signal");
      var1.setAttribute("everyTick", "false");
      Element var2 = new Element("signals");
      var1.addContent(var2);
      var2.addContent(this.getEntrySignalCondition(1, "RandomConditionLong", null));
      var2.addContent(this.getEntrySignalCondition(-1, "RandomConditionShort", "RandomConditionLong"));
      if (this.containsExitRule) {
         Element var3 = this.getExitSignalCondition(1, "RandomConditionLongExit", null);
         if (var3 != null) {
            var2.addContent(var3);
         }

         var3 = this.getExitSignalCondition(-1, "RandomConditionShortExit", "RandomConditionLongExit");
         if (var3 != null) {
            var2.addContent(var3);
         }
      }

      return var1;
   }

   protected Element getEntrySignalCondition(int var1, String var2, String var3) throws JDOMException, IOException, Exception {
      Element var4 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/signalentry.xml"));
      var4.setAttribute("variable", var1 == 1 ? "33333333-1111-1111-3333-333333333333" : "33333333-2222-1111-3333-333333333333");
      if ((var1 != 1 || !this.marketSides.equals("short")) && (var1 != -1 || !this.marketSides.equals("long"))) {
         Element var7 = var4.getChild("Item");
         Element var6 = var7.getChild("Param");
         if (var1 != 1 && this.entrySymmetry) {
            var7.setAttribute("key", "NegatedCondition");
            var7.setAttribute("name", "NegatedCondition");
            var7.setAttribute("display", "NegatedCondition(#Identification#)");
            var7.setAttribute("generated", "negated");
            var6.addContent(var3);
         } else {
            var7.setAttribute("key", "RandomCondition");
            var7.setAttribute("name", "RandomCondition");
            var7.setAttribute("display", "RandomCondition(@Chart@#Identification#)");
            var7.setAttribute("generated", "random");
            var6.addContent(var2);
            var6.setAttribute("ownRandomKey", "true");
         }
      } else {
         var4.removeContent();
         Element var5 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/itemfalse.xml"));
         var4.addContent(var5.detach());
      }

      return var4.detach();
   }

   protected Element getExitSignalCondition(int var1, String var2, String var3) throws JDOMException, IOException, Exception {
      Element var4 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/signalexit.xml"));
      var4.setAttribute("variable", var1 == 1 ? "33333333-1111-2222-3333-333333333333" : "33333333-2222-2222-3333-333333333333");
      if ((var1 != 1 || !this.marketSides.equals("short")) && (var1 != -1 || !this.marketSides.equals("long"))) {
         Element var7 = var4.getChild("Item");
         Element var6 = var7.getChild("Param");
         if (var1 != 1 && this.exitSymmetry) {
            var7.setAttribute("key", "NegatedCondition");
            var7.setAttribute("name", "NegatedCondition");
            var7.setAttribute("display", "NegatedCondition(#Identification#)");
            var7.setAttribute("generated", "negated");
            var6.addContent(var3);
         } else {
            var7.setAttribute("key", "RandomCondition");
            var7.setAttribute("name", "RandomCondition");
            var7.setAttribute("display", "RandomCondition(#Identification#)");
            var7.setAttribute("generated", "random");
            var6.addContent(var2);
            var6.setAttribute("ownRandomKey", "true");
         }
      } else {
         var4.removeContent();
         Element var5 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sq4/itemfalse.xml"));
         var4.addContent(var5.detach());
      }

      return var4.detach();
   }
}
