package com.strategyquant.tradinglib;

import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;
import com.strategyquant.tradinglib.riskmanagement.RiskManagementMethodsList;
import java.util.List;
import org.jdom2.Element;

public abstract class RiskManagementMethod extends ParametersTableItem implements ISQCloneable<RiskManagementMethod> {
   private double equity;
   private int decimals;

   public void setName(String var1) {
      this.name = var1;
   }

   protected double getEquity() {
      return this.equity;
   }

   protected int getDecimals() {
      return this.decimals;
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("Method");
      var1.setAttribute("type", this.getClass().getSimpleName());
      Element var2 = super.getXML();
      var1.addContent(var2);
      return var1;
   }

   @Override
   public void setFromXML(Element var1) throws Exception {
      Element var2 = this.getUsedSubelement(var1, "Method");
      if (var2 != null) {
         super.setFromXML(var1.getChild("Method"));
      } else {
         throw new Exception("Error while loading Risk Management xml config. XML element Method was not found.");
      }
   }

   public abstract void verifyOrder(Trader var1, ILiveOrder var2);

   public RiskManagementMethod getClone() throws Exception {
      try {
         for (RiskManagementMethod var3 : RiskManagementMethodsList.get().getAvailableClasses()) {
            if (var3.getClass().equals(this.getClass())) {
               RiskManagementMethod var4 = (RiskManagementMethod)var3.getClass().newInstance();
               List var5 = var4.getParametersList();

               for (int var6 = 0; var6 < var5.size(); var6++) {
                  String var7 = (String)var5.get(var6);
                  var4.setParameterValue(var7, "" + this.getParameterValue(var7));
               }

               return var4;
            }
         }
      } catch (Exception var8) {
         throw new CloneNotSupportedException("Cloning RiskManagementMethod " + this.name + " failed. " + var8.getMessage());
      }

      throw new CloneNotSupportedException("Cloning failed. Cannot find RiskManagementMethod " + this.name);
   }
}
