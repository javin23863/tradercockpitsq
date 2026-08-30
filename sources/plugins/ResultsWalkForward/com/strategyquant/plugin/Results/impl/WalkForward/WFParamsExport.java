package com.strategyquant.plugin.Results.impl.WalkForward;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WFParamsExport {
   private static final Logger Log = LoggerFactory.getLogger(WFParamsExport.class);

   public void toXlsx(String var1, boolean var2, WalkForwardResult var3, DatabankTableView var4) {
      XSSFWorkbook var5 = null;

      try {
         var5 = new XSSFWorkbook();
         Sheet var6 = var5.createSheet("Tradelist");
         Font var7 = var5.createFont();
         var7.setBold(true);
         var7.setFontHeightInPoints((short)14);
         var7.setColor(IndexedColors.RED.getIndex());
         CellStyle var8 = var5.createCellStyle();
         var8.setFont(var7);
         Row var9 = var6.createRow(0);
         Cell var10 = var9.createCell(1);
         var10.setCellValue("Period IS");
         var10.setCellStyle(var8);
         var10 = var9.createCell(2);
         var10.setCellValue("Period OOS");
         var10.setCellStyle(var8);
         var10 = var9.createCell(3);
         var10.setCellValue("Days IS");
         var10.setCellStyle(var8);
         var10 = var9.createCell(4);
         var10.setCellValue("Days OOS");
         var10.setCellStyle(var8);

         int var11;
         for (var11 = 0; var11 < var4.columns.size(); var11++) {
            var10 = var9.createCell(var11 + 5);
            var10.setCellValue(((DatabankTableColumnEntry)var4.columns.get(var11)).getColumnEntryName());
            var10.setCellStyle(var8);
         }

         var10 = var9.createCell(var11 + 5);
         var10.setCellValue("Parameters");
         var10.setCellStyle(var8);
         ArrayList var12 = var3.wfPeriods;

         for (int var44 = 0; var44 < var12.size(); var44++) {
            Row var13 = var6.createRow(var44 + 1);
            WalkForwardPeriod var14 = (WalkForwardPeriod)var12.get(var44);
            var10 = var13.createCell(1);
            var10.setCellValue(SQTime.toDateString(var14.optimizeFrom) + " - " + SQTime.toDateString(var14.optimizeTo));
            var10 = var13.createCell(2);
            if (var44 == var12.size() - 1) {
               var10.setCellValue(SQTime.toDateString(var14.runFrom) + " - " + SQTime.toDateString(var14.runTo) + " (future)");
            } else {
               var10.setCellValue(SQTime.toDateString(var14.runFrom) + " - " + SQTime.toDateString(var14.runTo));
            }

            var10 = var13.createCell(3);
            var10.setCellValue(SQTime.getDaysBetween(var14.optimizeFrom, var14.optimizeTo));
            var10 = var13.createCell(4);
            var10.setCellValue(SQTime.getDaysBetween(var14.runFrom, var14.runTo));
            ArrayList var15 = var4.columns;

            int var16;
            for (var16 = 0; var16 < var15.size(); var16++) {
               DatabankTableColumnEntry var17 = (DatabankTableColumnEntry)var15.get(var16);
               DatabankColumn var18 = var17.tableColumn;
               var10 = var13.createCell(var16 + 5);

               try {
                  String var19 = var17.sampleType == 10 ? var18.printValue(var14.optimizationStatData) : var18.printValue(var14.runStatData);
                  String var20 = this.formatValue(var17, var19, var2);
                  var10.setCellValue(var20);
               } catch (Exception var30) {
                  var10.setCellValue("N/A");
               }
            }

            var10 = var13.createCell(var16 + 5);
            var10.setCellValue(var14.testParameters);
         }

         for (int var45 = 0; var45 < var4.columns.size(); var45++) {
            var6.autoSizeColumn(var45);
         }

         if (!var1.toLowerCase().endsWith(".xlsx")) {
            var1 = var1 + ".xlsx";
         }

         FileOutputStream var46 = new FileOutputStream(var1);
         var5.write(var46);
         var46.close();
      } catch (Exception var31) {
         Log.error("Error while exporting WF params to xlsx.", var31);
      } finally {
         try {
            if (var5 != null) {
               var5.close();
            }
         } catch (Exception var29) {
            Log.error("Cannot close workbook", var29);
         }
      }
   }

   public void toCsv(String var1, boolean var2, WalkForwardResult var3, DatabankTableView var4) {
      try {
         if (!var1.toLowerCase().endsWith(".csv")) {
            var1 = var1 + ".csv";
         }

         PrintWriter var5 = new PrintWriter(var1);
         var5.print("\"Period IS\";");
         var5.print("\"Period OOS\";");
         var5.print("\"Days IS\";");
         var5.print("\"Days OOS\";");
         int var6 = var4.columns.size();

         for (int var7 = 0; var7 < var6; var7++) {
            var5.print("\"" + ((DatabankTableColumnEntry)var4.columns.get(var7)).getColumnEntryName() + "\";");
         }

         var5.print("\"Parameters\"");
         var5.println();
         ArrayList var18 = var3.wfPeriods;

         for (int var8 = 0; var8 < var18.size(); var8++) {
            WalkForwardPeriod var9 = (WalkForwardPeriod)var18.get(var8);
            var5.print("\"" + SQTime.toDateString(var9.optimizeFrom) + " - " + SQTime.toDateString(var9.optimizeTo) + "\";");
            if (var8 == var18.size() - 1) {
               var5.print("\"" + SQTime.toDateString(var9.runFrom) + " - " + SQTime.toDateString(var9.runTo) + " (future)\";");
            } else {
               var5.print("\"" + SQTime.toDateString(var9.runFrom) + " - " + SQTime.toDateString(var9.runTo) + "\";");
            }

            var5.print("\"" + SQTime.getDaysBetween(var9.optimizeFrom, var9.optimizeTo) + "\";");
            var5.print("\"" + SQTime.getDaysBetween(var9.runFrom, var9.runTo) + "\";");
            ArrayList var10 = var4.columns;

            for (int var11 = 0; var11 < var10.size(); var11++) {
               DatabankTableColumnEntry var12 = (DatabankTableColumnEntry)var10.get(var11);
               DatabankColumn var13 = var12.tableColumn;

               try {
                  String var14 = var12.sampleType == 10 ? var13.printValue(var9.optimizationStatData) : var13.printValue(var9.runStatData);
                  String var15 = this.formatValue(var12, var14, var2);
                  var5.print("\"" + var15 + "\";");
               } catch (Exception var16) {
                  var5.print("\"N/A\";");
               }
            }

            var5.print("\"" + var9.testParameters + "\"");
            var5.println();
         }

         var5.flush();
         var5.close();
      } catch (Exception var17) {
         Log.error("Error while exporting WF params to csv.", var17);
      }
   }

   private String formatValue(DatabankTableColumnEntry var1, String var2, boolean var3) {
      var2 = SQUtils.removeHtmlTags(var2);
      if (var3) {
         switch (var1.tableColumn.getType()) {
            case "Integer":
            case "Text":
            case "Time":
               return var2;
            default:
               return var2.replace('.', ',');
         }
      } else {
         return var2;
      }
   }
}
