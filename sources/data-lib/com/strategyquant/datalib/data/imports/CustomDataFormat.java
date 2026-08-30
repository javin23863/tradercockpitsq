package com.strategyquant.datalib.data.imports;

import com.strategyquant.datalib.data.io.columns.DefaultCol;
import java.util.HashMap;

public class CustomDataFormat {
   private String name;
   private String separator;
   private String dateFormat;
   private String timeFormat = null;
   private int skipRows;
   private int skipColumns;
   private boolean predefined = false;
   private HashMap<Integer, DefaultCol> columns;
   private int order = 0;

   public CustomDataFormat() {
      this(null);
   }

   public CustomDataFormat(String var1) {
      this.columns = new HashMap<>();
      this.name = var1;
   }

   public CustomDataFormat(String var1, String var2, String var3, int var4, int var5) {
      this.columns = new HashMap<>();
      this.name = var1;
      this.separator = var2;
      this.dateFormat = var3;
      this.skipRows = var4;
      this.skipColumns = var5;
   }

   public CustomDataFormat(String var1, String var2, String var3, int var4, int var5, HashMap<Integer, DefaultCol> var6) {
      this(var1, var2, var3, var4, var5);
      this.columns = var6;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getSeparator() {
      return this.separator;
   }

   public void setSeparator(String var1) {
      this.separator = var1;
   }

   public String getDateFormat() {
      return this.dateFormat;
   }

   public void setDateFormat(String var1) {
      this.dateFormat = var1;
   }

   public String getTimeFormat() {
      return this.timeFormat;
   }

   public void setTimeFormat(String var1) {
      this.timeFormat = var1;
   }

   public int getSkipRows() {
      return this.skipRows;
   }

   public void setSkipRows(int var1) {
      this.skipRows = var1;
   }

   public int getSkipColumns() {
      return this.skipColumns;
   }

   public void setSkipColumns(int var1) {
      this.skipColumns = var1;
   }

   public HashMap<Integer, DefaultCol> getColumns() {
      return this.columns;
   }

   public void setColumns(HashMap<Integer, DefaultCol> var1) {
      this.columns = var1;
   }

   public boolean isPredefined() {
      return this.predefined;
   }

   public void setPredefined(boolean var1) {
      this.predefined = var1;
   }

   public int getOrder() {
      return this.order;
   }

   public void setOrder(int var1) {
      this.order = var1;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public String columTypesToString() {
      String var1 = "";

      for (int var3 : this.columns.keySet()) {
         DefaultCol var4 = this.columns.get(var3);
         var1 = var1 + var4.getName() + ",";
      }

      return var1.length() > 0 ? var1.substring(0, var1.length() - 1) : var1;
   }
}
