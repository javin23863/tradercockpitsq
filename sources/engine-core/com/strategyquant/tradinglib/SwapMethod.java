package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import java.io.Serializable;
import org.jdom2.Element;

public class SwapMethod implements IXMLAble, Serializable {
   private boolean use;
   private String type;
   private double longSwap;
   private double shortSwap;
   private String tripleSwapOn;
   private String rolloutHour;
   private int[] rolloutHourHHMM;

   public SwapMethod() {
   }

   public SwapMethod(boolean var1, String var2, double var3, double var5, String var7, String var8) {
      this.use = var1;
      this.type = var2;
      this.longSwap = var3;
      this.shortSwap = var5;
      this.tripleSwapOn = var7;
      this.rolloutHour = var8;

      try {
         this.rolloutHourHHMM = SQTime.getHHMM(var8);
      } catch (Exception var10) {
         this.rolloutHourHHMM = new int[]{23, 0};
      }
   }

   public Element getXML() {
      Element var1 = new Element("Swap");
      var1.setAttribute("use", String.valueOf(this.use));
      var1.setAttribute("type", this.type != null ? this.type : "");
      var1.setAttribute("long", String.valueOf(this.longSwap));
      var1.setAttribute("short", String.valueOf(this.shortSwap));
      var1.setAttribute("tripleSwapOn", this.tripleSwapOn != null ? this.tripleSwapOn : "");
      var1.setAttribute("rolloutHour", this.rolloutHour != null ? this.rolloutHour : "23:00");
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.use = XMLUtil.getBooleanAttr(var1, "use", false);
      this.type = XMLUtil.getStringAttr(var1, "type", null);
      this.longSwap = XMLUtil.getDoubleAttr(var1, "long", 0.0);
      this.shortSwap = XMLUtil.getDoubleAttr(var1, "short", 0.0);
      this.tripleSwapOn = XMLUtil.getStringAttr(var1, "tripleSwapOn", null);
      this.rolloutHour = XMLUtil.getStringAttr(var1, "rolloutHour", "23:00");

      try {
         this.rolloutHourHHMM = SQTime.getHHMM(this.rolloutHour);
      } catch (Exception var3) {
         this.rolloutHourHHMM = new int[]{23, 0};
      }
   }

   public boolean isUsed() {
      return this.use;
   }

   public String getType() {
      return this.type;
   }

   public double getLongSwap() {
      return this.longSwap;
   }

   public double getShortSwap() {
      return this.shortSwap;
   }

   public String getTripleSwapOn() {
      return this.tripleSwapOn;
   }

   public int getRolloutHour() {
      try {
         return this.rolloutHourHHMM[0];
      } catch (Exception var2) {
         return 23;
      }
   }

   public int getRolloutMinute() {
      try {
         return this.rolloutHourHHMM[1];
      } catch (Exception var2) {
         return 0;
      }
   }

   public SwapMethod clone() {
      SwapMethod var1 = new SwapMethod();
      var1.use = this.use;
      var1.type = this.type;
      var1.longSwap = this.longSwap;
      var1.shortSwap = this.shortSwap;
      var1.tripleSwapOn = this.tripleSwapOn;
      var1.rolloutHour = this.rolloutHour;
      return var1;
   }

   public String getTypeSymbol() {
      return SwapTypes.toString(this.getType());
   }

   public String printFormatedName() {
      return this.isUsed()
         ? String.format(
            "Long %s%s, Short: %s%s, Triple on %s", this.getLongSwap(), this.getTypeSymbol(), this.getShortSwap(), this.getTypeSymbol(), this.getTripleSwapOn()
         )
         : "No swap";
   }
}
