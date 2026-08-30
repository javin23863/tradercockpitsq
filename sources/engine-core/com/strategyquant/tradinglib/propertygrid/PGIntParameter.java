package com.strategyquant.tradinglib.propertygrid;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class PGIntParameter implements IPGParameter {
   private String key;
   private String name;
   private String category;
   private String activator;
   private String description;
   private int min;
   private int max;
   private int step;
   private int value;
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

   public int getMin() {
      return this.min;
   }

   public void setMin(int var1) {
      this.min = var1;
   }

   public int getMax() {
      return this.max;
   }

   public void setMax(int var1) {
      this.max = var1;
   }

   public int getStep() {
      return this.step;
   }

   public void setStep(int var1) {
      this.step = var1;
   }

   public int getValue() {
      return this.value;
   }

   public void setValue(int var1) {
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

   public Element getXML() {
      Element var1 = new Element("Param");
      XMLUtil.trySetAttr(var1, "key", this.key);
      XMLUtil.trySetAttr(var1, "name", this.name);
      XMLUtil.trySetAttr(var1, "dataType", String.valueOf(1));
      XMLUtil.trySetAttr(var1, "min", String.valueOf(this.min));
      XMLUtil.trySetAttr(var1, "max", String.valueOf(this.max));
      XMLUtil.trySetAttr(var1, "step", String.valueOf(this.step));
      XMLUtil.trySetAttr(var1, "value", String.valueOf(this.value));
      XMLUtil.trySetAttr(var1, "description", this.description);
      if (this.activator != null) {
         XMLUtil.trySetAttr(var1, "activator", this.activator);
      }

      return var1;
   }

   public void setFromXML(Element var1) {
      this.key = var1.getAttributeValue("key");
      this.min = XMLUtil.tryGetIntAttr(var1, "min");
      this.max = XMLUtil.tryGetIntAttr(var1, "max");
      this.step = XMLUtil.tryGetIntAttr(var1, "step");
      this.value = XMLUtil.tryGetIntAttr(var1, "value");
      this.description = var1.getAttributeValue("description");
      this.activator = var1.getAttributeValue("activator");
   }

   @Override
   public int getType() {
      return 1;
   }

   @Override
   public String print() {
      return this.value + "";
   }

   @Override
   public void setValue(String var1) {
      this.value = Integer.parseInt(var1);
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
