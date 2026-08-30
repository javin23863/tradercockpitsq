package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.StringComparator;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsComparator implements Comparator<ResultsGroup> {
   private static final Logger Log = LoggerFactory.getLogger(ResultsComparator.class);
   private Databank databank;
   private StringComparator stringComparator = new StringComparator();
   private int column = 0;
   private String dataType;
   private boolean asc = true;

   public ResultsComparator(Databank var1) {
      this.databank = var1;
   }

   public int compare(ResultsGroup var1, ResultsGroup var2) {
      DatabankTableView var3 = this.databank.getView();
      if (var3 == null) {
         return 0;
      }

      this.dataType = "Text";
      String var4 = null;
      String var5 = null;
      boolean var6 = this.databank.getProject().equals("Optimizer");
      int var7 = this.column - (var6 ? 2 : 1);
      if (var7 >= 0 && var7 < var3.columns.size()) {
         DatabankTableColumnEntry var8 = var3.columns.get(var7);
         this.dataType = var8.tableColumn.getType();
         var4 = var8.getValue(var1);
         var5 = var8.getValue(var2);
      } else if (var7 == -1) {
         var4 = var6 ? var1.getParams() : var1.getName();
         var5 = var6 ? var2.getParams() : var2.getName();
      } else {
         if (var7 != -2) {
            Log.error("Invalid databank sort column index : " + this.column);
            return 0;
         }

         var4 = var1.getName();
         var5 = var2.getName();
      }

      try {
         switch (this.dataType) {
            case "Text":
               String var22 = SQUtils.removeHtmlTags(var4);
               String var23 = SQUtils.removeHtmlTags(var5);
               int var12 = this.stringComparator.compare(var22, var23);
               if (this.asc) {
                  return var12;
               }

               return -1 * var12;
            default:
               double var13 = Double.parseDouble(SQUtils.extractNumber(SQUtils.removeHtmlTags(var4)));
               double var15 = Double.parseDouble(SQUtils.extractNumber(SQUtils.removeHtmlTags(var5)));
               if (var13 < var15) {
                  return this.asc ? -1 : 1;
               } else if (var13 > var15) {
                  return this.asc ? 1 : -1;
               } else {
                  return 0;
               }
         }
      } catch (Exception var17) {
         String var9 = SQUtils.removeHtmlTags(String.valueOf(var4));
         String var10 = SQUtils.removeHtmlTags(String.valueOf(var5));
         int var11 = this.stringComparator.compare(var9, var10);
         return this.asc ? var11 : -1 * var11;
      }
   }

   public int getColumn() {
      return this.column;
   }

   public void setColumn(int var1) {
      this.column = var1;
   }

   public boolean isAsc() {
      return this.asc;
   }

   public void setAsc(boolean var1) {
      this.asc = var1;
   }
}
