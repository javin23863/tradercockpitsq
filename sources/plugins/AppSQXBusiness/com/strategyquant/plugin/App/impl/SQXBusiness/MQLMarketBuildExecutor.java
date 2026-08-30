package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.sqxbusiness.EAOption;
import com.strategyquant.lib.sqxbusiness.MQLMarketBuildSettings;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.jdom2.Element;

public class MQLMarketBuildExecutor {
   private static final Comparator<Element> variablesComparator = new Comparator<Element>() {
      public int compare(Element var1, Element var2) {
         int var3 = XMLUtil.getIntAttr(var1, "position", 0);
         int var4 = XMLUtil.getIntAttr(var2, "position", 0);
         return var3 - var4;
      }
   };
   private static GridClient gridClient;
   private static String jobGroupID;

   public static void init() {
      gridClient = SQGrid.getGridClient();
   }

   public static void execute(String var0, Element var1, String var2, String var3) throws Exception {
      jobGroupID = UUID.randomUUID().toString();
      List var4 = prepareJobs(var0, var1, var2, var3);
      SQXBusinessBuildReporter.registerMessageListener(jobGroupID);
      gridClient.executeOnGrid(jobGroupID, var4);
   }

   private static List<MQLMarketBuildJob> prepareJobs(String var0, Element var1, String var2, String var3) throws Exception {
      ArrayList var4 = new ArrayList();
      Element var5 = getStrategyXML(var1);
      CustomDataManager.addCDataIndySourceCodes(var5);
      CustomBlocks.addCBlockSourceCodes(var5);
      Element var6 = XMLUtil.getChildElem(var1, "Settings");
      Element var7 = XMLUtil.getChildElem(var6, "General");
      Element var8 = XMLUtil.getChildElem(var6, "Build");
      Element var9 = var6.getChild("EAOptions");
      String var10 = var8.getAttributeValue("outputName");
      MQLMarketBuildSettings var11 = new MQLMarketBuildSettings();
      var11.projectName = var0;
      var11.strategyXML = var5;
      var11.copyright = var7.getAttributeValue("copyright");
      var11.link = var7.getAttributeValue("link");
      var11.version = var7.getAttributeValue("version");
      var11.comment = var7.getAttributeValue("comment");
      var11.description = var7.getAttributeValue("description");
      var11.eaOptions = new ArrayList();
      if (var9 != null) {
         List var12 = var9.getChildren("Option");

         for (int var13 = 0; var13 < var12.size(); var13++) {
            Element var14 = (Element)var12.get(var13);
            String var15 = XMLUtil.getAttr(var14, "name", "xx");
            String var16 = XMLUtil.getAttr(var14, "type", "string");
            String var17 = XMLUtil.getAttr(var14, "value", "");
            boolean var18 = XMLUtil.getBooleanAttr(var14, "enabled", true);
            var11.eaOptions.add(new EAOption(var15, var16, var17, var18));
         }
      }

      boolean var19 = var2.equals("all");
      if (var19 || var2.equals("unlocked")) {
         Element var20 = XMLUtil.getChildElem(var8, "UnlockedEA");
         if (!var19 || XMLUtil.getBooleanAttr(var20, "use", false)) {
            var11.account = -1;
            var11.demoOnly = false;
            var11.expirationDate = null;
            var11.fixedSize = -1.0;
            tryAddJobs(var4, var0, var10, var20, var11, var3);
         }
      }

      if (var19 || var2.equals("lockedToDemo")) {
         Element var21 = XMLUtil.getChildElem(var8, "LockedToDemo");
         if (!var19 || XMLUtil.getBooleanAttr(var21, "use", false)) {
            var11.account = -1;
            var11.demoOnly = true;
            var11.expirationDate = null;
            var11.fixedSize = -1.0;
            tryAddJobs(var4, var0, var10, var21, var11, var3);
         }
      }

      if (var19 || var2.equals("fixedSize")) {
         Element var22 = XMLUtil.getChildElem(var8, "LockedToFixedSize");
         if (!var19 || XMLUtil.getBooleanAttr(var22, "use", false)) {
            var11.account = -1;
            var11.demoOnly = false;
            var11.expirationDate = null;
            var11.fixedSize = XMLUtil.getDoubleAttr(var22, "lots", 0.01);
            tryAddJobs(var4, var0, var10, var22, var11, var3);
         }
      }

      if (var19 || var2.equals("lockedByDate")) {
         Element var23 = XMLUtil.getChildElem(var8, "LockedByDate");
         if (!var19 || XMLUtil.getBooleanAttr(var23, "use", false)) {
            var11.account = -1;
            var11.demoOnly = false;
            var11.expirationDate = var23.getAttributeValue("date");
            var11.fixedSize = -1.0;
            tryAddJobs(var4, var0, var10, var23, var11, var3);
         }
      }

      if (var19 || var2.equals("demoLockedByDate")) {
         Element var24 = XMLUtil.getChildElem(var8, "LockedByDateDemo");
         if (!var19 || XMLUtil.getBooleanAttr(var24, "use", false)) {
            var11.account = -1;
            var11.demoOnly = true;
            var11.expirationDate = var24.getAttributeValue("date");
            var11.fixedSize = -1.0;
            tryAddJobs(var4, var0, var10, var24, var11, var3);
         }
      }

      if (var19 || var2.equals("lockedByAccount")) {
         Element var25 = XMLUtil.getChildElem(var8, "LockedByAccount");
         if (!var19 || XMLUtil.getBooleanAttr(var25, "use", false)) {
            Element var26 = XMLUtil.getChildElem(var25, "Accounts");
            List var27 = var26.getChildren("Account");

            for (int var28 = 0; var28 < var27.size(); var28++) {
               Element var29 = (Element)var27.get(var28);
               if (XMLUtil.getBooleanAttr(var29, "use", false)) {
                  var11.account = XMLUtil.getIntAttr(var29, "number", 0);
                  var11.demoOnly = XMLUtil.getBooleanAttr(var29, "demoOnly", false);
                  var11.expirationDate = var29.getAttributeValue("validUntil");
                  var11.fixedSize = -1.0;
                  tryAddJobs(var4, var0, var10, var29, var11, var3);
               }
            }
         }
      }

      return var4;
   }

   private static void tryAddJobs(List<MQLMarketBuildJob> var0, String var1, String var2, Element var3, MQLMarketBuildSettings var4, String var5) throws Exception {
      String var6 = var2 + var3.getAttributeValue("name");
      if (var5.equals("mql4") || var5.equals("mql45")) {
         MQLMarketBuildSettings var7 = var4.clone();
         var7.platform = "mql4";
         var7.outputName = var6;
         HashMap var8 = new HashMap();
         var8.put("settings", var7);
         String var9 = MQLMarketConst.getBuildJobID(var1, var6, false);
         var0.add(new MQLMarketBuildJob(var9, 1, var8));
         SQXBusinessBuildReporter.addNewBuildData(var1, jobGroupID, var9, var6, false);
      }

      if (var5.equals("mql5") || var5.equals("mql45")) {
         MQLMarketBuildSettings var10 = var4.clone();
         var10.platform = "mql5";
         var10.outputName = var6;
         HashMap var11 = new HashMap();
         var11.put("settings", var10);
         String var12 = MQLMarketConst.getBuildJobID(var1, var6, true);
         var0.add(new MQLMarketBuildJob(var12, 1, var11));
         SQXBusinessBuildReporter.addNewBuildData(var1, jobGroupID, var12, var6, true);
      }
   }

   private static Element getStrategyXML(Element var0) throws Exception {
      ResultsGroup var1 = EAParameters.getParametrizedResultsGroup(var0);
      Element var2 = StrategyXMLModifier.getUpdatedStrategyXML(var1);
      HashMap var3 = getEAParameters(var0);
      ArrayList var4 = new ArrayList();
      Element var5 = XMLUtil.getChildElem(var2, "Strategy");
      Element var6 = XMLUtil.getChildElem(var5, "Variables");
      List var7 = var6.getChildren("variable");

      for (int var8 = var7.size() - 1; var8 >= 0; var8--) {
         Element var9 = (Element)var7.get(var8);
         String var10 = var9.getChildText("name");
         if (!var10.equals("MagicNumber")) {
            if (var3.containsKey(var10)) {
               Element var11 = var9.clone();
               EAParameter var12 = (EAParameter)var3.get(var10);
               XMLUtil.getChildElem(var11, "name").setText(var12.newName);
               XMLUtil.getChildElem(var11, "value").setText(var12.value);
               XMLUtil.getChildElem(var11, "makeExternal").setText("true");
               var11.setAttribute("position", "" + var12.position);
               var4.add(var11);
               var6.removeContent(var9);
            } else {
               Element var23 = XMLUtil.getChildElem(var9, "makeExternal");
               var23.setText("false");
            }
         }
      }

      int var18 = 1;

      for (String var21 : var3.keySet()) {
         EAParameter var24 = (EAParameter)var3.get(var21);
         if (var24.isCategory) {
            Element var25 = new Element("variable");
            Element var13 = new Element("id");
            var13.setText("");
            var25.addContent(var13);
            Element var14 = new Element("name");
            var14.setText("cat" + var18++);
            var25.addContent(var14);
            Element var15 = new Element("type");
            var15.setText("string");
            var25.addContent(var15);
            Element var16 = new Element("value");
            var16.setText(var24.newName);
            var25.addContent(var16);
            Element var17 = new Element("makeExternal");
            var17.setText("true");
            var25.addContent(var17);
            var25.setAttribute("position", "" + var24.position);
            var4.add(var25);
         }
      }

      var4.sort(variablesComparator);

      for (int var20 = 0; var20 < var4.size(); var20++) {
         Element var22 = (Element)var4.get(var20);
         var6.addContent(var22);
      }

      return var2;
   }

   private static HashMap<String, EAParameter> getEAParameters(Element var0) throws Exception {
      HashMap var1 = new HashMap();
      Element var2 = XMLUtil.getChildElem(var0, "Settings");
      Element var3 = XMLUtil.getChildElem(var2, "Parameters");
      Element var4 = XMLUtil.getChildElem(var3, "Params");
      List var5 = var4.getChildren("Param");

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         if (XMLUtil.getBooleanAttr(var7, "external", false)) {
            EAParameter var8 = new EAParameter(var7);
            var1.put(var8.originalName, var8);
         }
      }

      return var1;
   }
}
