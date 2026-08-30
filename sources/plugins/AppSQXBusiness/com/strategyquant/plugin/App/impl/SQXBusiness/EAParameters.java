package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.lib.L;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import org.jdom2.Element;

public class EAParameters {
   public static ResultsGroup getParametrizedResultsGroup(Element var0) throws Exception {
      Element var1 = XMLUtil.getChildElem(var0, "Settings");
      Element var2 = XMLUtil.getChildElem(var1, "Parameters");
      Element var3 = XMLUtil.getChildElem(var2, "VariableTypes");
      String var4 = XMLUtil.getAttr(var2, "parametrizationType", "recommended");
      boolean var5 = var4.equals("recommended");
      boolean var6 = XMLUtil.getBooleanAttr(var3, "symmetric", true);
      if (var4.equals("none")) {
         return getStrategy(var0);
      }

      ValuesMap var7 = new ValuesMap();
      if (var5) {
         var7.set("ParamTypeRecommended", true);
      } else {
         var7.set("ParamTypePeriod", XMLUtil.getNodeValue(var3, "Periods").equals("true"));
         var7.set("ParamTypeShift", XMLUtil.getNodeValue(var3, "Shifts").equals("true"));
         var7.set("ParamTypeConstant", XMLUtil.getNodeValue(var3, "Constants").equals("true"));
         var7.set("ParamTypeOtherParam", XMLUtil.getNodeValue(var3, "OtherParams").equals("true"));
         var7.set("ParamTypeExitUsed", XMLUtil.getNodeValue(var3, "ExitUsed").equals("true"));
         var7.set("ParamTypeExitUnused", XMLUtil.getNodeValue(var3, "ExitUnused").equals("true"));
         var7.set("ParamTypeBoolean", XMLUtil.getNodeValue(var3, "Boolean").equals("true"));
         var7.set("ParamTypeTradingOptions", XMLUtil.getNodeValue(var3, "TradingOptions", "false").equals("true"));
         var7.set("ParamTypeEntryLevel", XMLUtil.getNodeValue(var3, "Entry", "false").equals("true"));
         var7.set("ParamTypeEntryLogic", XMLUtil.getNodeValue(var3, "EntryLogic", "false").equals("true"));
      }

      ResultsGroup var8 = getStrategy(var0);
      StrategyBase var9 = StrategyBase.createXmlStrategy(var8.getStrategyXml());
      var9.transformToVariables(var6 && var9.isSymmetryEnabled(), var7);
      return var8;
   }

   public static StrategyBase getStrategyBase(Element var0) throws Exception {
      Element var1 = XMLUtil.getChildElem(var0, "Settings");
      Element var2 = XMLUtil.getChildElem(var1, "Parameters");
      Element var3 = XMLUtil.getChildElem(var2, "VariableTypes");
      String var4 = XMLUtil.getAttr(var2, "parametrizationType", "recommended");
      boolean var5 = var4.equals("recommended");
      boolean var6 = XMLUtil.getBooleanAttr(var3, "symmetric", true);
      if (var4.equals("none")) {
         return null;
      }

      ValuesMap var7 = new ValuesMap();
      if (var5) {
         var7.set("ParamTypeRecommended", true);
      } else {
         var7.set("ParamTypePeriod", XMLUtil.getNodeValue(var3, "Periods").equals("true"));
         var7.set("ParamTypeShift", XMLUtil.getNodeValue(var3, "Shifts").equals("true"));
         var7.set("ParamTypeConstant", XMLUtil.getNodeValue(var3, "Constants").equals("true"));
         var7.set("ParamTypeOtherParam", XMLUtil.getNodeValue(var3, "OtherParams").equals("true"));
         var7.set("ParamTypeExitUsed", XMLUtil.getNodeValue(var3, "ExitUsed").equals("true"));
         var7.set("ParamTypeExitUnused", XMLUtil.getNodeValue(var3, "ExitUnused").equals("true"));
         var7.set("ParamTypeBoolean", XMLUtil.getNodeValue(var3, "Boolean").equals("true"));
         var7.set("ParamTypeTradingOptions", XMLUtil.getNodeValue(var3, "TradingOptions", "false").equals("true"));
         var7.set("ParamTypeEntryLevel", XMLUtil.getNodeValue(var3, "Entry", "false").equals("true"));
         var7.set("ParamTypeEntryLogic", XMLUtil.getNodeValue(var3, "EntryLogic", "false").equals("true"));
      }

      ResultsGroup var8 = getStrategy(var0);
      StrategyBase var9 = StrategyBase.createXmlStrategy(var8.getStrategyXml());
      var9.transformToVariables(var6 && var9.isSymmetryEnabled(), var7);
      return var9;
   }

   public static ResultsGroup getStrategy(Element var0) throws Exception {
      Element var1 = XMLUtil.getChildElem(var0, "Settings");
      Element var2 = XMLUtil.getChildElem(var1, "General");
      String var3 = var2.getAttributeValue("name");
      String var4 = var2.getAttributeValue("strategy");
      String var5 = MQLMarketConst.getProjectDirPath(var3);
      String var6 = var5 + "/" + var4;
      if (var4 != null && !var4.isEmpty()) {
         IProgram var7 = Program.get("Loader");
         return (ResultsGroup)var7.call("loadFile", new Object[]{var6});
      } else {
         throw new Exception(L.t("Source strategy not found. Please set it in General settings", new Object[0]));
      }
   }
}
