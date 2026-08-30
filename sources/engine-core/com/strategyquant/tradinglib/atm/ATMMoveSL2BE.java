package com.strategyquant.tradinglib.atm;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import org.jdom2.Element;

public class ATMMoveSL2BE {
   public boolean MoveSL2BE;
   public boolean MoveSL2BEAddPips;
   public int Type;
   public int AddPipsValue;

   public ATMMoveSL2BE(Element var1) {
      this.MoveSL2BE = XMLUtil.getItemBoolParam(var1, "MoveSL2BE", false);
      this.MoveSL2BEAddPips = XMLUtil.getItemBoolParam(var1, "MoveSL2BEAddPips", false);
      this.Type = XMLUtil.getItemIntParam(var1, "Type", 1);
      this.AddPipsValue = XMLUtil.getItemIntParam(var1, "AddPipsValue", 10);
   }

   public void apply(LiveOrderObj var1) {
   }

   public Element getXML() {
      Element var1 = new Element("MoveSL2BE");
      XMLUtil.setItemBoolParam(var1, "MoveSL2BE", this.MoveSL2BE);
      XMLUtil.setItemBoolParam(var1, "MoveSL2BEAddPips", this.MoveSL2BEAddPips);
      XMLUtil.setItemIntParam(var1, "Type", this.Type);
      XMLUtil.setItemIntParam(var1, "AddPipsValue", this.AddPipsValue);
      return var1;
   }

   @Override
   public String toString() {
      String var1 = "Move SL 2 BE";
      if (this.MoveSL2BEAddPips && this.Type == 1) {
         var1 = var1 + String.format(" + %d pips", this.AddPipsValue);
      }

      return var1;
   }
}
