package com.strategyquant.tradinglib.propertygrid;

import com.strategyquant.lib.XMLUtil;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jdom2.Element;

public class PGComboParameter implements IPGParameter {
   private String key;
   private String name;
   private String category;
   private String activator;
   private String description;
   private String value;
   private String engine;
   private TreeMap<String, String> options = new TreeMap<>();

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

   public String getValue() {
      return this.value;
   }

   @Override
   public void setValue(String var1) {
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

   public TreeMap<String, String> getOptions() {
      return this.options;
   }

   public void setOptions(TreeMap<String, String> var1) {
      this.options = var1;
   }

   public Element getXML() {
      Element var1 = new Element("Param");
      XMLUtil.trySetAttr(var1, "key", this.key);
      XMLUtil.trySetAttr(var1, "name", this.name);
      XMLUtil.trySetAttr(var1, "dataType", String.valueOf(5));
      XMLUtil.trySetAttr(var1, "value", String.valueOf(this.value));
      XMLUtil.trySetAttr(var1, "description", this.description);
      if (this.activator != null) {
         XMLUtil.trySetAttr(var1, "activator", this.activator);
      }

      Element var2 = new Element("Options");

      for (String var4 : this.options.keySet()) {
         Element var5 = new Element("Option");
         var5.setAttribute("name", this.options.get(var4));
         var5.setText(var4);
         var2.addContent(var5);
      }

      var1.addContent(var2);
      return var1;
   }

   public void setFromXML(Element var1) {
      this.key = var1.getAttributeValue("key");
      this.value = var1.getAttributeValue("value");
      this.description = var1.getAttributeValue("description");
      this.activator = var1.getAttributeValue("activator");
      Element var2 = var1.getChild("Options");
      if (var2 != null) {
         int var3 = var2.getChildren("Option").size();
         this.options.clear();

         for (int var4 = 0; var4 < var3; var4++) {
            Element var5 = (Element)var2.getChildren("Option").get(var4);
            String var6 = var5.getAttributeValue("name");
            String var7 = var5.getTextNormalize();
            this.options.put(var6, var7);
         }
      }
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public String print() {
      for (Entry var2 : this.options.entrySet()) {
         if (this.value.equals(var2.getValue())) {
            return (String)var2.getKey();
         }
      }

      return this.value;
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
