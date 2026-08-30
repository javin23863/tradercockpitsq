package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.propertygrid.ParametersTableItem;
import java.lang.reflect.Constructor;
import java.util.List;
import org.jdom2.Element;

public abstract class MoneyManagementMethod extends ParametersTableItem implements ISQCloneable<MoneyManagementMethod> {
   public String name = null;
   private double equity;
   private double initialCapital = 10000.0;
   public static final double defaultInitialCapital = 10000.0;
   protected double weight = 1.0;
   protected double maxPos = 1.0;

   public void setName(String var1) {
      this.name = var1;
   }

   protected double getEquity() {
      return this.equity;
   }

   public double getInitialCapital() {
      return this.initialCapital;
   }

   public void setInitialCapital(double var1) {
      this.initialCapital = var1;
   }

   @Override
   public Element getXML() {
      Element var1 = new Element("MoneyManagement");
      Element var2 = new Element("Method");
      var2.setAttribute("type", this.getClass().getSimpleName());
      var2.setAttribute("use", "true");
      Element var3 = super.getXML();
      var2.addContent(var3);
      Element var4 = new Element("InitialCapital");
      var4.setText(String.valueOf(this.initialCapital));
      var1.addContent(var2);
      var1.addContent(var4);
      return var1;
   }

   @Override
   public void setFromXML(Element var1) throws Exception {
      Element var2 = this.getUsedSubelement(var1, "Method");
      if (var2 != null) {
         this.setName(var2.getAttributeValue("type"));
         super.setFromXML(var2);
         Element var3 = var1.getChild("InitialCapital");
         if (var3 != null) {
            try {
               this.setInitialCapital(Double.parseDouble(var3.getTextTrim()));
            } catch (Exception var5) {
               Log.error("Invalid Initial Capital settings. Using default value - 10000.0");
            }
         } else {
            Log.warn("Initial capital XML element not found");
         }
      } else {
         throw new Exception(L.t("Cannot set up MoneyManagement method. Method xml element not found.", new Object[0]));
      }
   }

   public MoneyManagementMethod newInstance() throws Exception {
      Constructor var1 = this.getClass().getConstructor();
      return (MoneyManagementMethod)SQUtils.invokeUnchecked(var1, new Object[0]);
   }

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10, double var12) throws Exception {
      return 0.0;
   }

   public double computeTradeSize(StrategyBase var1, String var2, byte var3, double var4, double var6, double var8, double var10) throws Exception {
      return this.computeTradeSize(var1, var2, var3, var4, var6, var8, var10, 0.0);
   }

   public void setMethodFromXML(Element var1) throws Exception {
      for (Element var3 : var1.getChild("Params").getChildren("Param")) {
         this.setFromParameterEl(var3);
      }
   }

   @Override
   public void setFromParameterEl(Element var1) throws Exception {
      String var2 = var1.getAttributeValue("key");
      var2 = SQUtils.renameActionParams(var2);
      var2 = var2.replaceAll("#", "");
      String var3 = var1.getTextTrim();
      if (var3 == null || var3.isEmpty()) {
         var3 = var1.getAttributeValue("value");
      }

      this.setParameterValue(var2, var3);
   }

   public MoneyManagementMethod getClone() throws Exception {
      try {
         for (MoneyManagementMethod var3 : MoneyManagementMethodsList.get().getAvailableClasses()) {
            if (var3.getClass().equals(this.getClass())) {
               MoneyManagementMethod var4 = (MoneyManagementMethod)var3.getClass().newInstance();
               List var5 = var4.getParametersList();

               for (int var6 = 0; var6 < var5.size(); var6++) {
                  String var7 = (String)var5.get(var6);
                  var4.setParameterValue(var7, "" + this.getParameterValue(var7));
               }

               return var4;
            }
         }
      } catch (Exception var8) {
         throw new CloneNotSupportedException("Cloning MoneyManagementMethod " + this.name + " failed. " + var8.getMessage());
      }

      throw new CloneNotSupportedException("Cloning failed. Cannot find MoneyManagementMethod " + this.name);
   }

   public double round(double var1, double var3, int var5) {
      if (var3 > 0.0) {
         var1 = Math.floor(var1 / var3) * var3;
      }

      return SQUtils.round(var1, var5);
   }

   public void setWeight(double var1) {
      this.weight = var1;
   }

   public void setMaxOpenPositions(double var1) {
      this.maxPos = var1;
   }
}
