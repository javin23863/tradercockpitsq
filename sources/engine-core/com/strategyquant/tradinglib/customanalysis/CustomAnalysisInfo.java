package com.strategyquant.tradinglib.customanalysis;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import java.io.Serializable;
import org.jdom2.Element;

public class CustomAnalysisInfo implements Serializable {
   public boolean filter = false;
   public String inputArgs;
   public CustomAnalysisMethod method = null;
   public CustomAnalysisMethod perStrategy1 = null;
   public CustomAnalysisMethod perStrategy2 = null;
   public CustomAnalysisMethod fullDatabank1 = null;
   public CustomAnalysisMethod fullDatabank2 = null;
   public String inputArgsPerStrategy1;
   public String inputArgsPerStrategy2;
   public String inputArgsFullDatabank1;
   public String inputArgsFullDatabank2;

   public void loadRankingConfig(Element var1) throws Exception {
      if (var1 != null) {
         this.filter = XMLUtil.getBooleanAttr(var1, "filter", false);
         this.inputArgs = XMLUtil.getStringAttr(var1, "inputArgs", "");
         String var2 = XMLUtil.getStringAttr(var1, "method", "none");
         if (!var2.equals("none")) {
            this.method = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var2);
         }
      }
   }

   public void loadTaskConfig(Element var1) throws Exception {
      if (var1 != null) {
         this.filter = XMLUtil.getNodeBooleanValue(var1, "Filter", false);
         this.inputArgsPerStrategy1 = XMLUtil.getNodeValue(var1, "InputArgsPerStrategy1", "");
         this.inputArgsPerStrategy2 = XMLUtil.getNodeValue(var1, "InputArgsPerStrategy2", "");
         this.inputArgsFullDatabank1 = XMLUtil.getNodeValue(var1, "InputArgsFullDatabank1", "");
         this.inputArgsFullDatabank2 = XMLUtil.getNodeValue(var1, "InputArgsFullDatabank2", "");
         String var2 = XMLUtil.getNodeValue(var1, "PerStrategy1", "none");
         if (!var2.equals("none")) {
            this.perStrategy1 = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var2);
         }

         var2 = XMLUtil.getNodeValue(var1, "PerStrategy2", "none");
         if (!var2.equals("none")) {
            this.perStrategy2 = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var2);
         }

         var2 = XMLUtil.getNodeValue(var1, "FullDatabank1", "none");
         if (!var2.equals("none")) {
            this.fullDatabank1 = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var2);
         }

         var2 = XMLUtil.getNodeValue(var1, "FullDatabank2", "none");
         if (!var2.equals("none")) {
            this.fullDatabank2 = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var2);
         }
      }
   }
}
