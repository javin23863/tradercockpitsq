package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankExport {
   private static final Logger Log = LoggerFactory.getLogger(DatabankExport.class);

   public int toCsv(Databank var1, String var2, String var3, boolean var4, String var5, String var6) throws Exception {
      int var7 = 0;

      try {
         if (!var2.toLowerCase().endsWith(".csv") && !var2.toLowerCase().endsWith(".txt")) {
            var2 = var2 + ".csv";
         }

         File var8 = new File(var2).getParentFile();
         if (!var8.exists()) {
            var8.mkdirs();
         }

         PrintWriter var9 = new PrintWriter(var2);
         DatabankTableView var10 = var1.getView();
         if (var5 != null) {
            var10 = DatabankViewsManager.getInstance().getView(var5);
         }

         int var11 = var10.columns.size();
         var9.print(this.enclose(var6, "Strategy Name", true));
         String var12 = var1.getProject();
         if (!var12.equals("Builder") && !var12.equals("PortfolioMaster")) {
            var9.print(this.enclose(var6, "Filters result", true));
         }

         if (var12.equals("Optimizer")) {
            var9.print(this.enclose(var6, "Parameters", true));
            var9.print(this.enclose(var6, "Best WF", true));
         }

         for (int var13 = 0; var13 < var11; var13++) {
            var9.print(this.enclose(var6, var10.columns.get(var13).getColumnEntryName(), var13 != var11 - 1));
         }

         var9.println();
         ArrayList var34 = this.getRecordKeys(var1, var3);

         for (int var14 = 0; var14 < var34.size(); var14++) {
            ResultsGroup var15 = null;

            try {
               var15 = var1.getLocked((String)var34.get(var14), "DatabankExport");
            } catch (Exception var32) {
               continue;
            }

            try {
               var9.print(this.enclose(var6, var15.getName(), true));
               if (!var12.equals("Builder") && !var12.equals("PortfolioMaster")) {
                  try {
                     DatabankColumn var16 = (DatabankColumn)DatabankColumns.get().findClassByName("FiltersResult");
                     String var17 = var16.exportValue(var15, null, (byte)0, (byte)10, (byte)127);
                     var9.print(this.enclose(var6, var17, true));
                  } catch (Exception var30) {
                     var9.print(this.enclose(var6, "N/A", true));
                  }
               }

               if (var12.equals("Optimizer")) {
                  try {
                     WalkForwardMatrixResult var36 = (WalkForwardMatrixResult)var15.mainResult().get("WalkForwardResult");
                     WalkForwardResult var39 = var36.getWFResult(var15.getBestWFResultKey(), true);
                     String var18 = var4 ? var39.testParams.replace(',', '/').replace('.', ',') : var39.testParams;
                     var9.print(this.enclose(var6, var18, true));
                     var9.print(this.enclose(var6, var39.toString(var36.periodType), true));
                  } catch (Exception var29) {
                     String var38 = "N/A";

                     try {
                        var38 = var15.getParams();
                        var38 = var4 ? var38.replace(',', '/').replace('.', ',') : var38;
                     } catch (Exception var28) {
                     }

                     var9.print(this.enclose(var6, var38, true));
                     var9.print(this.enclose(var6, "N/A", true));
                  }
               }

               for (int var37 = 0; var37 < var10.columns.size(); var37++) {
                  DatabankTableColumnEntry var40 = var10.columns.get(var37);

                  try {
                     var9.print(this.enclose(var6, "" + this.formatValue(var40, var15, var4), false));
                  } catch (Exception var27) {
                     var9.print(this.enclose(var6, "N/A", false));
                  }

                  if (var37 != var11 - 1) {
                     var9.print(var6);
                  }
               }

               var9.println();
               var7++;
            } finally {
               if (var15 != null) {
                  var15.releaseLock("DatabankExport");
               }
            }
         }

         var9.flush();
         var9.close();
         return var7;
      } catch (Exception var33) {
         Log.error("Error while exporting databank '" + var1.getName() + "' to csv.", var33);
         throw new Exception(L.t("Error while exporting databank '%s' to xlsx.", new Object[]{var1.getName()}), var33);
      }
   }

   private String enclose(String var1, String var2, boolean var3) {
      String var4 = "";
      if (!var1.equals("\t")) {
         var4 = "\"";
      }

      return var4 + var2 + var4 + (var3 ? var1 : "");
   }

   public void toXlsx(Databank var1, String var2, String var3, boolean var4, String var5) throws Exception {
      XSSFWorkbook var6 = null;

      try {
         var6 = new XSSFWorkbook();
         Sheet var7 = var6.createSheet(var1.getName() + " databank");
         Font var8 = var6.createFont();
         var8.setBold(true);
         var8.setFontHeightInPoints((short)14);
         var8.setColor(IndexedColors.RED.getIndex());
         CellStyle var9 = var6.createCellStyle();
         var9.setFont(var8);
         CellStyle var10 = var6.createCellStyle();
         var10.setDataFormat((short)1);
         CellStyle var11 = var6.createCellStyle();
         var11.setDataFormat((short)2);
         DatabankTableView var12 = var1.getView();
         if (var5 != null) {
            var12 = DatabankViewsManager.getInstance().getView(var5);
         }

         String var13 = var1.getProject();
         int var14 = 0;
         Row var15 = var7.createRow(0);
         Cell var16 = var15.createCell(var14++);
         var16.setCellValue("Strategy name");
         var16.setCellStyle(var9);
         if (!var13.equals("Builder") && !var13.equals("PortfolioMaster")) {
            var16 = var15.createCell(var14++);
            var16.setCellValue("Filters result");
            var16.setCellStyle(var9);
         }

         if (var13.equals("Optimizer")) {
            var16 = var15.createCell(var14++);
            var16.setCellValue("Parameters");
            var16.setCellStyle(var9);
            var16 = var15.createCell(var14++);
            var16.setCellValue("Best WF");
            var16.setCellStyle(var9);
         }

         for (int var17 = 0; var17 < var12.columns.size(); var17++) {
            var16 = var15.createCell(var17 + var14);
            var16.setCellValue(var12.columns.get(var17).getColumnEntryName());
            var16.setCellStyle(var9);
         }

         ArrayList var74 = this.getRecordKeys(var1, var3);
         CellStyle var18 = var6.createCellStyle();
         var18.setDataFormat((short)1);

         for (int var19 = 0; var19 < var74.size(); var19++) {
            ResultsGroup var20 = null;

            try {
               var20 = var1.getLocked((String)var74.get(var19), "DatabankExport");
            } catch (Exception var53) {
               continue;
            }

            try {
               Row var21 = var7.createRow(var19 + 1);
               var14 = 0;
               var16 = var21.createCell(var14++);
               var16.setCellValue(var20.getName());
               if (!var13.equals("Builder") && !var13.equals("PortfolioMaster")) {
                  var16 = var21.createCell(var14++);

                  try {
                     DatabankColumn var22 = (DatabankColumn)DatabankColumns.get().findClassByName("FiltersResult");
                     String var23 = var22.exportValue(var20, null, (byte)0, (byte)10, (byte)127);
                     var16.setCellValue(var23);
                  } catch (Exception var51) {
                     var16.setCellValue("N/A");
                  }
               }

               if (var13.equals("Optimizer")) {
                  try {
                     WalkForwardMatrixResult var79 = (WalkForwardMatrixResult)var20.mainResult().get("WalkForwardResult");
                     WalkForwardResult var82 = var79.getWFResult(var20.getBestWFResultKey(), true);
                     String var24 = var4 ? var82.testParams.replace(',', '/').replace('.', ',') : var82.testParams;
                     var16 = var21.createCell(var14++);
                     var16.setCellValue(var24);
                     var16 = var21.createCell(var14++);
                     var16.setCellValue(var82.toString(var79.periodType));
                  } catch (Exception var50) {
                     String var81 = "N/A";

                     try {
                        var81 = var20.getParams();
                        var81 = var4 ? var81.replace(',', '/').replace('.', ',') : var81;
                     } catch (Exception var49) {
                     }

                     var16 = var21.createCell(var14++);
                     var16.setCellValue(var81);
                     var16 = var21.createCell(var14++);
                     var16.setCellValue("N/A");
                  }

                  var16 = var15.createCell(2);
                  var16.setCellValue("Parameters");
                  var16.setCellStyle(var9);
                  var16 = var15.createCell(3);
                  var16.setCellValue("Best WF");
                  var16.setCellStyle(var9);
               }

               for (int var80 = 0; var80 < var12.columns.size(); var80++) {
                  var16 = var21.createCell(var80 + var14);
                  DatabankTableColumnEntry var83 = var12.columns.get(var80);

                  try {
                     this.setFormatedCellValue(var16, var83, var20, var6, var10, var11);
                  } catch (Exception var48) {
                     var16.setCellValue("N/A");
                  }
               }
            } finally {
               if (var20 != null) {
                  var20.releaseLock("DatabankExport");
               }
            }
         }

         for (int var75 = 0; var75 < var12.columns.size(); var75++) {
            var7.autoSizeColumn(var75);
         }

         if (!var2.toLowerCase().endsWith(".xlsx")) {
            var2 = var2 + ".xlsx";
         }

         File var76 = new File(var2).getParentFile();
         if (!var76.exists()) {
            var76.mkdirs();
         }

         FileOutputStream var78 = new FileOutputStream(var2);
         var6.write(var78);
         var78.close();
      } catch (Exception var54) {
         Log.error("Error while exporting databank '" + var1.getProject() + "/" + var1.getName() + "' to MS Office XLSX." + var54.getMessage(), var54);
         throw new Exception(
            L.t("Error while exporting databank '%s/%s' to MS Office XLSX - %s", new Object[]{var1.getProject(), var1.getName(), var54.getMessage()}), var54
         );
      } finally {
         try {
            if (var6 != null) {
               var6.close();
            }
         } catch (Exception var47) {
            Log.error("Cannot close workbook", var47);
         }
      }
   }

   private void setFormatedCellValue(Cell var1, DatabankTableColumnEntry var2, ResultsGroup var3, Workbook var4, CellStyle var5, CellStyle var6) {
      String var7 = var2.exportValue(var3);
      switch (var2.tableColumn.getType()) {
         case "Text":
         case "Time":
            var1.setCellValue(var7);
            break;
         case "Integer":
            if (var7 != null && !var7.equals("")) {
               var1.setCellStyle(var5);

               try {
                  var1.setCellValue(Integer.parseInt(var7));
               } catch (NumberFormatException var11) {
                  var1.setCellValue((int)Double.parseDouble(var7));
               }
            } else {
               var1.setCellValue(var7);
            }
            break;
         default:
            if (var7 != null && !var7.equals("")) {
               var1.setCellStyle(var6);
               var1.setCellValue(Double.parseDouble(var7));
            } else {
               var1.setCellValue(var7);
            }
      }
   }

   private String formatValue(DatabankTableColumnEntry var1, ResultsGroup var2, boolean var3) {
      String var4 = var1.exportValue(var2);
      if (var3) {
         switch (var1.tableColumn.getType()) {
            case "Integer":
            case "Text":
            case "Time":
               return var4;
            default:
               return var4.replace('.', ',');
         }
      } else {
         return var4;
      }
   }

   private ArrayList<String> getRecordKeys(Databank var1, String var2) {
      if (var2 != null) {
         if (var2 == "none") {
            return new ArrayList<>();
         }

         if (!var2.isEmpty()) {
            return new ArrayList<>(Arrays.asList(var2.split(",")));
         }
      }

      return var1.getRecordKeys();
   }
}
