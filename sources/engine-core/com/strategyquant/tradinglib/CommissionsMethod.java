package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;
import org.jdom2.Element;

public abstract class CommissionsMethod extends ParametersTableItem {
   public String name = null;

   public void setName(String var1) {
      this.name = var1;
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("Commissions");
      Element var2 = new Element("Method");
      var2.setAttribute("type", this.getClass().getSimpleName());
      Element var3 = super.getXML();
      var2.addContent(var3);
      var1.addContent(var2);
      return var1;
   }

   @Override
   public void setFromXML(Element var1) throws Exception {
      Element var2 = this.getUsedSubelement(var1, "Method");
      if (var2 != null) {
         this.setName(var2.getAttributeValue("type"));
         super.setFromXML(var2);
      } else {
         throw new Exception("Cannot set up MoneyManagement method. Method xml element not found");
      }
   }

   public CommissionsMethod getClone() throws Exception {
      return (CommissionsMethod)super.getClone();
   }

   public abstract double computeCommissionsOnOpen(ILiveOrder var1, double var2, double var4) throws Exception;

   public abstract double computeCommissionsOnClose(ILiveOrder var1, double var2, double var4) throws Exception;
}
