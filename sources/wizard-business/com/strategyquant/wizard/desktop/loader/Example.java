package com.strategyquant.wizard.desktop.loader;

public class Example {
   private String name;
   private String desc;
   private String file;
   private String image;
   private boolean cloud;

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getDesc() {
      return this.desc;
   }

   public void setDesc(String var1) {
      this.desc = var1;
   }

   public String getFile() {
      return this.file;
   }

   public void setFile(String var1) {
      this.file = var1;
   }

   public String getImage() {
      return this.image;
   }

   public void setImage(String var1) {
      this.image = var1;
   }

   public String toJson() {
      String var1 = String.valueOf(this.cloud);
      return "{\"name\":\""
         + this.name
         + "\", \"desc\":\""
         + this.desc
         + "\",\"id\":\""
         + this.file
         + "\",\"image\":"
         + this.getImageJson()
         + ",\"tour\":null}";
   }

   private String getImageJson() {
      return this.image == null ? "null" : "\"" + this.image + "\"";
   }

   public boolean isCloud() {
      return this.cloud;
   }

   public void setCloud(boolean var1) {
      this.cloud = var1;
   }
}
