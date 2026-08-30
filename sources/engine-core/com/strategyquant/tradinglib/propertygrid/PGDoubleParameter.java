package com.strategyquant.tradinglib.propertygrid;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class PGDoubleParameter implements IPGParameter {
   private String key;
   private String name;
   private String category;
   private String activator;
   private String description;
   private double min;
   private double max;
   private double step;
   private double value;
   private int decimals;
   private String engine;

   @Override
   public String getKey() {
      return this.key;
   }

   public void setKey(String var1) {
      this.key = var1;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String var1) {
      this.description = var1;
   }

   public double getMin() {
      return this.min;
   }

   public void setMin(double var1) {
      this.min = var1;
   }

   public double getMax() {
      return this.max;
   }

   public void setMax(double var1) {
      this.max = var1;
   }

   public double getStep() {
      return this.step;
   }

   public void setStep(double var1) {
      this.step = var1;
   }

   public double getValue() {
      return this.value;
   }

   public void setValue(double var1) {
      this.value = var1;
   }

   @Override
   public String getCategory() {
      return this.category;
   }

   @Override
   public void setCategory(String var1) {
      this.category = var1;
   }

   @Override
   public String getEngine() {
      return this.engine;
   }

   @Override
   public void setEngine(String var1) {
      this.engine = var1;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public int getDecimals() {
      return this.decimals;
   }

   public void setDecimals(int var1) {
      this.decimals = var1;
   }

   public Element getXML() {
      Element var1 = new Element("Param");
      XMLUtil.trySetAttr(var1, "key", this.key);
      XMLUtil.trySetAttr(var1, "name", this.name);
      XMLUtil.trySetAttr(var1, "dataType", String.valueOf(2));
      XMLUtil.trySetAttr(var1, "min", String.valueOf(this.min));
      XMLUtil.trySetAttr(var1, "max", String.format("%f", this.max));
      XMLUtil.trySetAttr(var1, "step", String.valueOf(this.step));
      XMLUtil.trySetAttr(var1, "value", String.valueOf(this.value));
      XMLUtil.trySetAttr(var1, "description", this.description);
      if (this.activator != null) {
         XMLUtil.trySetAttr(var1, "activator", this.activator);
      }

      XMLUtil.trySetAttr(var1, "decimals", String.valueOf(this.decimals));
      return var1;
   }

   public void setFromXML(Element var1) {
      this.key = var1.getAttributeValue("key");
      this.min = XMLUtil.tryGetDoubleAttr(var1, "min");
      this.max = XMLUtil.tryGetDoubleAttr(var1, "max");
      this.step = XMLUtil.tryGetDoubleAttr(var1, "step");
      this.value = XMLUtil.tryGetDoubleAttr(var1, "value");
      this.description = var1.getAttributeValue("description");
      this.activator = var1.getAttributeValue("activator");
      this.decimals = XMLUtil.tryGetIntAttr(var1, "decimals");
   }

   @Override
   public int getType() {
      return 2;
   }

   @Override
   public String print() {
      return SQUtils.d2String(this.value, this.decimals);
   }

   @Override
   public void setValue(String var1) {
      this.value = Double.parseDouble(var1);
   }

   @Override
   public String getActivator() {
      return this.activator;
   }

   @Override
   public void setActivator(String var1) {
      this.activator = var1;
   }
}
