package com.strategyquant.tradinglib;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.atm.ATMMoveSL2BE;
import com.strategyquant.tradinglib.atm.exits.ATMExitLevels;
import com.strategyquant.tradinglib.atm.exits.AbstractExitLevel;
import com.strategyquant.tradinglib.atm.sizes.ATMSizes;
import com.strategyquant.tradinglib.atm.sizes.AbstractATMPositionSize;
import java.io.Serializable;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ATMExit implements Serializable {
   private static final Logger Log = LoggerFactory.getLogger(ATMExit.class);
   public AbstractATMPositionSize positionSize;
   public AbstractExitLevel exitLevel;
   public ATMMoveSL2BE moveSL2BE;
   private int exitIndex;
   private Element elExit;
   private int sizeDecimals;
   private double minimalSize;

   public ATMExit(int var1, Element var2, int var3, double var4) throws Exception {
      try {
         this.exitIndex = var1;
         this.elExit = var2;
         this.sizeDecimals = var3;
         this.minimalSize = var4;
         Element var6 = XMLUtil.getChildElem(var2, "PositionSizes");
         List var7 = var6.getChildren("PositionSize");

         for (int var8 = 0; var8 < var7.size(); var8++) {
            Element var9 = (Element)var7.get(var8);
            if (XMLUtil.elementIs(var9, "use")) {
               this.positionSize = ATMSizes.create(var9, var3, var4);
               break;
            }
         }

         Element var13 = XMLUtil.getChildElem(var2, "ExitLevels");
         List var14 = var13.getChildren("ExitLevel");

         for (int var10 = 0; var10 < var14.size(); var10++) {
            Element var11 = (Element)var14.get(var10);
            if (XMLUtil.elementIs(var11, "use")) {
               this.exitLevel = ATMExitLevels.create(var11, var1);
               break;
            }
         }

         if (this.exitLevel == null) {
            throw new Exception("Exit level not defined.");
         }

         Element var15 = XMLUtil.getChildElem(var2, "MoveSL2BE");
         this.moveSL2BE = new ATMMoveSL2BE(var15);
      } catch (Exception var12) {
         throw new Exception(String.format("ATM Exit #%d - %s", var1 + 1, var12.getMessage()));
      }
   }

   public double computeSize(double var1, double var3, boolean var5) {
      return var3 >= var1 ? 0.0 : Math.min(this.positionSize.computeSize(var1, var3, var5), SQUtils.round(var1 - var3, this.sizeDecimals));
   }

   public void setForOrder(ILiveOrder var1, StrategyBase var2, double var3, double var5) throws TradingException {
      this.exitLevel.setForOrder(var1, var2, var3, var5);
   }

   public static String toString(byte var0, byte var1) {
      return String.format("Exit #%d - %s", var1 + 1, OrderCloseTypes.toString(var0));
   }

   @Override
   public String toString() {
      return this.moveSL2BE.MoveSL2BE
         ? String.format("%s %s + %s", this.positionSize.toString(), this.exitLevel.toString(), this.moveSL2BE.toString())
         : String.format("%s %s", this.positionSize.toString(), this.exitLevel.toString());
   }

   public Element getXML() {
      Element var1 = new Element("Exit");
      Element var2 = new Element("PositionSizes");
      var1.addContent(var2);
      Element var3 = this.positionSize.getXML();
      var3.setAttribute("use", "true");
      var2.addContent(var3);
      Element var4 = new Element("ExitLevels");
      var1.addContent(var4);
      Element var5 = this.exitLevel.getXML();
      var5.setAttribute("use", "true");
      var4.addContent(var5);
      Element var6 = this.moveSL2BE.getXML();
      var1.addContent(var6);
      return var1;
   }

   public ATMExit clone() {
      try {
         return new ATMExit(this.exitIndex, this.elExit, this.sizeDecimals, this.minimalSize);
      } catch (Exception var2) {
         return null;
      }
   }
}
