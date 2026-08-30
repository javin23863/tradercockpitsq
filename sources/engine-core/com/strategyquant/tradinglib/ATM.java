package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.atm.ATMTypes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ATM implements Serializable {
   private static final Logger Log = LoggerFactory.getLogger(ATM.class);
   private int atmType;
   private int sizeDecimals = 1;
   private double minimalSize = 0.1;
   private ArrayList<ATMExit> exits;
   private ATMGenerateConfig generateConfig;

   public ATM(Element var1) throws Exception {
      this(var1, false);
   }

   public ATM(Element var1, boolean var2) throws Exception {
      try {
         this.sizeDecimals = XMLUtil.getIntAttr(var1, "sizeDecimals", 1);
         this.minimalSize = XMLUtil.getDoubleAttr(var1, "minSize", 0.1);
         this.exits = new ArrayList<>();
         boolean var3 = var2 ? true : XMLUtil.getBooleanAttr(var1, "enable", false);
         if (!var3) {
            this.atmType = 0;
         } else {
            this.atmType = ATMTypes.recognize(var1.getAttributeValue("scaleOutType"), 2);
            List var4 = var1.getChildren("ATM");
            boolean var5 = false;

            for (int var6 = 0; var6 < var4.size(); var6++) {
               Element var7 = (Element)var4.get(var6);
               String var8 = var7.getAttributeValue("id");
               if (var8 != null && var8.equals("global")) {
                  Element var9 = XMLUtil.getChildElem(var7, "Exits");
                  List var10 = var9.getChildren("Exit");

                  for (int var11 = 0; var11 < var10.size(); var11++) {
                     Element var12 = (Element)var10.get(var11);
                     ATMExit var13 = new ATMExit(var11, var12, this.sizeDecimals, this.minimalSize);
                     this.exits.add(var13);
                  }

                  var5 = true;
                  break;
               }
            }

            if (!var5) {
               throw new Exception(L.t("Global ATM config not found", new Object[0]));
            }

            Element var15 = var1.getChild("GenerateConfig");
            if (var15 != null) {
               this.generateConfig = new ATMGenerateConfig();
               this.generateConfig.init(var15, this.sizeDecimals, this.minimalSize);
            }
         }
      } catch (Exception var14) {
         Log.error("Cannot get ATM settings.", var14);
         throw new Exception("Cannot get ATM settings - " + var14.getMessage());
      }
   }

   public ATM getClone() {
      return this;
   }

   public static void setForOrder(ILiveOrder var0, StrategyBase var1) {
      if (!var0.isClosedOrder()) {
         ATM var2 = var1.getATM();
         if (var2 != null) {
         }
      }
   }

   public boolean isApplicable(StrategyBase var1, double var2, double var4, byte var6) {
      ATM var7 = var1.getATM();
      if (var7 == null) {
         return false;
      } else {
         return this.atmType == 0 ? false : var7.getExitsCount() > 0;
      }
   }

   public int getExitsCount() {
      return this.exits == null ? 0 : this.exits.size();
   }

   public ATMExit getExit(int var1) {
      return this.exits != null && var1 >= 0 && var1 < this.exits.size() ? this.exits.get(var1) : null;
   }

   public int getType() {
      return this.atmType;
   }

   public int getSizeDecimals() {
      return this.sizeDecimals;
   }

   public double getMinimalSize() {
      return this.minimalSize;
   }

   public static ATM createFromStrategy(Element var0) throws Exception {
      Element var1 = var0.getChild("ATMs");
      if (var1 == null) {
         return null;
      }

      List var2 = var1.getChildren("ATM");
      return var2 != null && !var2.isEmpty() ? new ATM(var1, true) : null;
   }

   public static Element setConfigATMToStrategy(ATM var0, Element var1, boolean var2) {
      if (var0 == null) {
         return var1;
      }

      Element var3 = var0.getXML();
      Element var4 = var1.getChild("ATMs");
      if (var4 != null) {
         var1.removeChild("ATMs");
      }

      Element var5 = new Element("ATMs");
      var1.addContent(var5);
      var5.setAttribute("enable", Boolean.toString(var2));
      var5.setAttribute("source", "config");
      var5.addContent(var3);
      return var1;
   }

   private Element getXML() {
      Element var1 = new Element("ATM");
      var1.setAttribute("id", "global");
      var1.setAttribute("sizeDecimals", Integer.toString(this.sizeDecimals));
      var1.setAttribute("minimalSize", Double.toString(this.minimalSize));
      Element var2 = new Element("Exits");
      var1.addContent(var2);
      if (this.exits != null) {
         for (int var3 = 0; var3 < this.exits.size(); var3++) {
            ATMExit var4 = this.exits.get(var3);
            Element var5 = var4.getXML();
            var2.addContent(var5);
         }
      }

      return var1;
   }

   public static Element setSourceAndEnable(Element var0, String var1, boolean var2) {
      Element var3 = var0.getChild("ATMs");
      if (var3 == null) {
         var3 = new Element("ATMs");
         var0.addContent(var3);
         if (var1 == null) {
            var3.setAttribute("source", "none");
         }
      }

      var3.setAttribute("enable", Boolean.toString(var2));
      if (var1 != null) {
         var3.setAttribute("source", var1);
      }

      return var0;
   }

   public boolean isUsed() {
      return this.atmType != 0;
   }

   public ATMGenerateConfig getGenerateConfig() {
      return this.generateConfig;
   }

   public void changeToStrategy() {
      this.atmType = 1;
   }
}
