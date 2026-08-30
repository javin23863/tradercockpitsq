package com.strategyquant.wizard.desktop.loader;

public class Template {
   private String id;
   private String name;
   private String descriptions;
   private String extension;
   private String form;

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getDescriptions() {
      return this.descriptions;
   }

   public void setDescriptions(String var1) {
      this.descriptions = var1;
   }

   public String getExtension() {
      return this.extension;
   }

   public void setExtension(String var1) {
      this.extension = var1;
   }

   public String toJson() {
      String var1 = this.form;
      if (var1 != null) {
         var1 = var1.replace("\t", "").replace("\n", "").replace("\r", "").replace("\"", "\\\"");
      } else {
         var1 = "";
      }

      return "{\"id\":\""
         + this.id
         + "\",\"name\":\""
         + this.name
         + "\",\"description\":\""
         + this.descriptions
         + "\",\"extension\":\""
         + this.extension
         + "\", \"form\":\"<form>"
         + var1
         + "</form>\"}";
   }

   public String getId() {
      return this.id;
   }

   public void setId(String var1) {
      this.id = var1;
   }

   public String getForm() {
      return this.form;
   }

   public void setForm(String var1) {
      this.form = var1;
   }
}
