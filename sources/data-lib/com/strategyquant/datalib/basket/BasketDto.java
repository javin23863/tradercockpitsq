package com.strategyquant.datalib.basket;

public class BasketDto {
   private String name;
   private String desc;
   private int count;
   private int total;
   private Integer id;
   private boolean system;

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

   public int getCount() {
      return this.count;
   }

   public void setCount(int var1) {
      this.count = var1;
   }

   public Integer getId() {
      return this.id;
   }

   public void setId(Integer var1) {
      this.id = var1;
   }

   public boolean isSystem() {
      return this.system;
   }

   public void setSystem(boolean var1) {
      this.system = var1;
   }

   public int getTotal() {
      return this.total;
   }

   public void setTotal(int var1) {
      this.total = var1;
   }

   public boolean isDefault() {
      return this.name.startsWith("[[");
   }
}
