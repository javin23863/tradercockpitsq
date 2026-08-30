package com.strategyquant.plugin.Results.impl.OptimizationProfile.results;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.OptimizationTestResult;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

public class OptResultsExport {
   private static final Logger Log = LoggerFactory.getLogger(OptResultsExport.class);

   public void toXlsx(String var1, boolean var2, ResultsGroup var3, String var4, String var5, String var6, String var7, String var8, DatabankTableView var9) {
      XSSFWorkbook var10 = null;

      try {
         var10 = new XSSFWorkbook();
         Sheet var11 = var10.createSheet("OptResults");
         Font var12 = var10.createFont();
         var12.setBold(true);
         var12.setFontHeightInPoints((short)14);
         var12.setColor(IndexedColors.RED.getIndex());
         CellStyle var13 = var10.createCellStyle();
         var13.setFont(var12);
         Row var14 = var11.createRow(0);

         int var16;
         for (var16 = 0; var16 < var9.columns.size(); var16++) {
            Cell var15 = var14.createCell(var16);
            var15.setCellValue(((DatabankTableColumnEntry)var9.columns.get(var16)).getColumnEntryName());
            var15.setCellStyle(var13);
         }

         if (var8.equals("3D")) {
            Cell var40 = var14.createCell(var16);
            var40.setCellValue(var4);
            var40.setCellStyle(var13);
            var40 = var14.createCell(var16 + 1);
            var40.setCellValue(var5);
            var40.setCellStyle(var13);
         } else if (var8.equals("2D")) {
            Cell var42 = var14.createCell(var16);
            var42.setCellValue(var4);
            var42.setCellStyle(var13);
         }

         OptimizationProfile var17 = var3.getOptimizationProfile();
         ObjectArrayList var18 = var17.getOptimizations().clone();

         for (int var50 = 0; var50 < var18.size(); var50++) {
            OptimizationTestResult var19 = (OptimizationTestResult)var18.get(var50);
            Int2DoubleOpenHashMap var20 = var19.parseParams();
            Row var21 = var11.createRow(var50 + 1);
            ArrayList var22 = var9.columns;

            int var23;
            for (var23 = 0; var23 < var22.size(); var23++) {
               DatabankTableColumnEntry var24 = (DatabankTableColumnEntry)var22.get(var23);
               DatabankColumn var25 = var24.tableColumn;
               Cell var43 = var21.createCell(var23);

               try {
                  String var26 = var25.printValue(var19.stats);
                  String var27 = this.formatValue(var24, var26, var2);
                  var43.setCellValue(var27);
               } catch (Exception var37) {
                  var43.setCellValue("N/A");
               }
            }

            if (var8.equals("3D")) {
               if (var20.containsKey(var4.hashCode())) {
                  double var53 = var20.get(var4.hashCode());
                  Cell var44 = var21.createCell(var23);
                  var44.setCellValue(var53);
               } else {
                  Cell var45 = var21.createCell(var23);
                  var45.setCellValue("N/A");
               }

               if (var20.containsKey(var5.hashCode())) {
                  double var54 = var20.get(var5.hashCode());
                  Cell var46 = var21.createCell(var23 + 1);
                  var46.setCellValue(var54);
               } else {
                  Cell var47 = var21.createCell(var23 + 1);
                  var47.setCellValue("N/A");
               }
            } else if (var8.equals("2D")) {
               if (var20.containsKey(var4.hashCode())) {
                  double var55 = var20.get(var4.hashCode());
                  Cell var48 = var21.createCell(var23);
                  var48.setCellValue(var55);
               } else {
                  Cell var49 = var21.createCell(var23);
                  var49.setCellValue("N/A");
               }
            }
         }

         for (int var51 = 0; var51 < var9.columns.size(); var51++) {
            var11.autoSizeColumn(var51);
         }

         if (!var1.toLowerCase().endsWith(".xlsx")) {
            var1 = var1 + ".xlsx";
         }

         FileOutputStream var52 = new FileOutputStream(var1);
         var10.write(var52);
         var52.close();
      } catch (Exception var38) {
         Log.error("Error while exporting Optimization Results to xlsx.", var38);
      } finally {
         try {
            if (var10 != null) {
               var10.close();
            }
         } catch (Exception var36) {
            Log.error("Cannot close workbook", var36);
         }
      }
   }

   public void toCsv(String var1, boolean var2, ResultsGroup var3, String var4, String var5, String var6, String var7, String var8, DatabankTableView var9) {
      try {
         if (!var1.toLowerCase().endsWith(".csv")) {
            var1 = var1 + ".csv";
         }

         PrintWriter var10 = new PrintWriter(var1);
         int var11 = var9.columns.size();

         for (int var12 = 0; var12 < var11; var12++) {
            var10.print("\"" + ((DatabankTableColumnEntry)var9.columns.get(var12)).getColumnEntryName() + "\";");
         }

         if (var8.equals("3D")) {
            var10.print("\"" + var4 + "\";");
            var10.print("\"" + var5 + "\"");
         } else if (var8.equals("2D")) {
            var10.print("\"" + var4 + "\"");
         }

         var10.println();
         OptimizationProfile var25 = var3.getOptimizationProfile();
         ObjectArrayList var13 = var25.getOptimizations().clone();

         for (int var14 = 0; var14 < var13.size(); var14++) {
            OptimizationTestResult var15 = (OptimizationTestResult)var13.get(var14);
            Int2DoubleOpenHashMap var16 = var15.parseParams();
            ArrayList var17 = var9.columns;

            for (int var18 = 0; var18 < var17.size(); var18++) {
               DatabankTableColumnEntry var19 = (DatabankTableColumnEntry)var17.get(var18);
               DatabankColumn var20 = var19.tableColumn;

               try {
                  String var21 = var20.printValue(var15.stats);
                  String var22 = this.formatValue(var19, var21, var2);
                  var10.print("\"" + var22 + "\";");
               } catch (Exception var23) {
                  var10.print("\"N/A\";");
               }
            }

            if (var8.equals("3D")) {
               if (var16.containsKey(var4.hashCode())) {
                  double var26 = var16.get(var4.hashCode());
                  var10.print("\"" + var26 + "\";");
               } else {
                  var10.print("\"N/A\";");
               }

               if (var16.containsKey(var5.hashCode())) {
                  double var27 = var16.get(var5.hashCode());
                  var10.print("\"" + var27 + "\"");
               } else {
                  var10.print("\"N/A\"");
               }
            } else if (var8.equals("2D")) {
               if (var16.containsKey(var4.hashCode())) {
                  double var28 = var16.get(var4.hashCode());
                  var10.print("\"" + var28 + "\"");
               } else {
                  var10.print("\"N/A\"");
               }
            }

            var10.println();
         }

         var10.flush();
         var10.close();
      } catch (Exception var24) {
         Log.error("Error while exporting Optimization Results to csv.", var24);
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
