package com.strategyquant.tradinglib.atm;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.task.settings.blocks.ConfigVerifier;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ATMConfigVerifier {
   private static final Logger Log = LoggerFactory.getLogger(ConfigVerifier.class);

   public static void verifyConfig(Element var0) throws Exception {
      verifyConfig(var0, null);
   }

   public static void verifyConfig(Element var0, TaskSettingsData var1) throws Exception {
      if (var0 == null) {
         addError(var1, L.t("Invalid XML configuration - missing Settings element.", new Object[0]));
      } else {
         try {
            checkATM(var0);
         } catch (Exception var3) {
            addError(var1, var3.getMessage());
         }
      }
   }

   public static void checkATM(Element var0) throws Exception {
      Element var1 = var0.getChild("RiskMoneyManagement");
      if (var1 != null) {
         double var2 = getFixedSizeFromMM(var1);
         if (var2 != 0.0) {
            Element var4 = var0.getChild("ATMs");
            if (var4 != null) {
               boolean var5 = XMLUtil.getBooleanAttr(var4, "enable", false);
               if (var5) {
                  int var6 = getNumberOfExits(var4);
                  if (var6 > 0) {
                     double var7 = XMLUtil.getDoubleAttr(var4, "minSize", 0.1);
                     if (var7 == var2 && var6 > 1) {
                        throw new Exception(
                           L.t("Minimal ATM exit size equals to your fixed size in Money Management, but you have multiple ATM exits.", new Object[0])
                              + L.t("This means it is not possible to use more than one exit !", new Object[0])
                        );
                     }
                  }
               }
            }
         }
      }
   }

   private static int getNumberOfExits(Element var0) {
      Element var1 = var0.getChild("ATM");
      if (var1 == null) {
         return 0;
      }

      Element var2 = var1.getChild("Exits");
      if (var2 == null) {
         return 0;
      }

      List var3 = var2.getChildren("Exit");
      return var3 == null ? 0 : var3.size();
   }

   private static double getFixedSizeFromMM(Element var0) {
      Element var1 = var0.getChild("MoneyManagement");
      if (var1 == null) {
         return 0.0;
      }

      List var2 = var1.getChildren("Method");
      if (var2 == null) {
         return 0.0;
      }

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("use");
         if (var5 != null && var5.equals("true")) {
            String var6 = var4.getAttributeValue("type");
            if (var6 != null && var6.equals("FixedSize")) {
               Element var7 = var4.getChild("Params");
               if (var7 != null) {
                  return XMLUtil.getItemDoubleParam(var7, "Size", 0.0);
               }
               break;
            }
         }
      }

      return 0.0;
   }

   private static void addError(TaskSettingsData var0, String var1) throws Exception {
      if (var0 == null) {
         throw new Exception(L.t("Money Management / ATM settings", new Object[0]) + " - " + var1);
      }

      var0.addError(L.t("Money Management / ATM settings", new Object[0]), null, var1);
   }
}
