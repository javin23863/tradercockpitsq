package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.StringComparator;
import java.util.Comparator;
import java.util.Map.Entry;

public class SortValuesComparator implements Comparator<Entry<String, Object>> {
   private StringComparator stringComparator = new StringComparator();
   private boolean asc = true;
   private String dataType = "Text";

   public int compare(Entry<String, Object> var1, Entry<String, Object> var2) {
      try {
         switch (this.dataType) {
            case "Text":
               String var14 = SQUtils.removeHtmlTags((String)var1.getValue());
               String var15 = SQUtils.removeHtmlTags((String)var2.getValue());
               int var7 = this.stringComparator.compare(var14, var15);
               if (this.asc) {
                  return var7;
               }

               return -1 * var7;
            default:
               double var8 = Double.parseDouble(SQUtils.extractNumber(SQUtils.removeHtmlTags((String)var1.getValue())));
               double var10 = Double.parseDouble(SQUtils.extractNumber(SQUtils.removeHtmlTags((String)var2.getValue())));
               if (var8 < var10) {
                  return this.asc ? -1 : 1;
               } else if (var8 > var10) {
                  return this.asc ? 1 : -1;
               } else {
                  return 0;
               }
         }
      } catch (Exception var12) {
         String var4 = SQUtils.removeHtmlTags(String.valueOf(var1.getValue()));
         String var5 = SQUtils.removeHtmlTags(String.valueOf(var2.getValue()));
         int var6 = this.stringComparator.compare(var4, var5);
         return this.asc ? var6 : -1 * var6;
      }
   }

   public void setAsc(boolean var1) {
      this.asc = var1;
   }

   public void setDataType(String var1) {
      this.dataType = var1;
   }
}
