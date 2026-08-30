package com.strategyquant.tradinglib.tradelist;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Databank;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradelistExport {
   private static final Logger Log = LoggerFactory.getLogger(TradelistExport.class);
   private HashMap<Short, CellStyle> cellStyles = new HashMap<>();

   public void toCsv(ArrayList<Object[]> var1, String var2, boolean var3) {
      try {
         if (!var2.toLowerCase().endsWith(".csv")) {
            var2 = var2 + ".csv";
         }

         String var4 = this.getCSVContent(var1, var3);
         SQUtils.stringToFile(var2, var4);
      } catch (Exception var5) {
         Log.error("Error while exporting tradelist to csv.", var5);
      }
   }

   public String toCsvCloud(ArrayList<Object[]> var1, boolean var2) {
      return this.getCSVContent(var1, var2);
   }

   private String getCSVContent(ArrayList<Object[]> var1, boolean var2) {
      StringBuilder var3 = new StringBuilder();
      TradelistTableView var4 = TradelistViewsManager.getInstance().getSelectedTradelistView();
      int var5 = var4.columns.size();

      for (int var6 = 0; var6 < var5; var6++) {
         var3.append("\"");
         var3.append(var4.columns.get(var6).getColumnEntryName());
         var3.append("\"");
         if (var6 != var5 - 1) {
            var3.append(";");
         }
      }

      var3.append("\r\n");

      for (int var13 = 0; var13 < var1.size(); var13++) {
         Object[] var7 = (Object[])var1.get(var13);

         for (int var8 = 0; var8 < var4.columns.size(); var8++) {
            Object var9 = var7[var8];
            TradelistTableColumnEntry var10 = var4.columns.get(var8);

            try {
               String var11 = this.formatValue(var10, String.valueOf(var9), var2);
               var3.append("\"");
               var3.append(var11);
               var3.append("\"");
            } catch (Exception var12) {
               var3.append("\"N/A\"");
            }

            if (var8 != var5 - 1) {
               var3.append(";");
            }
         }

         var3.append("\r\n");
      }

      return var3.toString();
   }

   public void toXlsx(ArrayList<Object[]> var1, String var2, boolean var3) throws Exception {
      XSSFWorkbook var4 = null;

      try {
         var4 = this.createWorkBook(var1);
         if (!var2.toLowerCase().endsWith(".xlsx")) {
            var2 = var2 + ".xlsx";
         }

         FileOutputStream var5 = new FileOutputStream(var2);
         var4.write(var5);
         var5.close();
      } catch (Exception var13) {
         Log.error("Error while exporting tradelist to xlsx.", var13);
         throw var13;
      } finally {
         try {
            if (var4 != null) {
               var4.close();
            }
         } catch (Exception var12) {
            Log.error("Cannot close workbook", var12);
         }
      }
   }

   public byte[] toXlsxCloud(ArrayList<Object[]> var1) throws Exception {
      XSSFWorkbook var2 = null;

      try {
         var2 = this.createWorkBook(var1);
         ByteArrayOutputStream var3 = new ByteArrayOutputStream();
         var2.write(var3);
         return var3.toByteArray();
      } catch (Exception var13) {
         Log.error("Error while exporting tradelist to xlsx.", var13);
         throw var13;
      } finally {
         try {
            if (var2 != null) {
               var2.close();
            }
         } catch (Exception var12) {
            Log.error("Cannot close workbook", var12);
         }
      }
   }

   private XSSFWorkbook createWorkBook(ArrayList<Object[]> var1) {
      XSSFWorkbook var2 = null;

      try {
         var2 = new XSSFWorkbook();
         XSSFSheet var3 = var2.createSheet("Tradelist");
         XSSFFont var4 = var2.createFont();
         var4.setBold(true);
         var4.setFontHeightInPoints((short)14);
         var4.setColor(IndexedColors.RED.getIndex());
         XSSFCellStyle var5 = var2.createCellStyle();
         var5.setFont(var4);
         XSSFDataFormat var6 = var2.createDataFormat();
         TradelistTableView var7 = TradelistViewsManager.getInstance().getSelectedTradelistView();
         Row var8 = var3.createRow(0);

         for (int var9 = 0; var9 < var7.columns.size(); var9++) {
            Cell var10 = var8.createCell(var9 + 1);
            var10.setCellValue(var7.columns.get(var9).getColumnEntryName());
            var10.setCellStyle(var5);
         }

         for (int var18 = 0; var18 < var1.size(); var18++) {
            Object[] var20 = (Object[])var1.get(var18);
            Row var11 = var3.createRow(var18 + 1);

            for (int var12 = 0; var12 < var7.columns.size(); var12++) {
               Object var13 = var20[var12];
               TradelistTableColumnEntry var14 = var7.columns.get(var12);
               Cell var15 = var11.createCell(var12 + 1);
               this.setCellValue(var14, String.valueOf(var13), var2, var15, var6);
            }
         }

         for (int var19 = 0; var19 < var7.columns.size(); var19++) {
            var3.autoSizeColumn(var19);
         }

         return var2;
      } catch (Exception var16) {
         Log.error("Error while exporting tradelist to xlsx.", var16);
         throw var16;
      }
   }

   private void setCellValue(TradelistTableColumnEntry var1, String var2, XSSFWorkbook var3, Cell var4, DataFormat var5) {
      String var6 = "";
      short var7 = 0;

      try {
         var6 = var1.getColumnEntryName();
         if (var2 == null) {
            var2 = "N/A";
         }

         String var8 = var1.tableColumn.getType();
         if (var2.equals("N/A")) {
            var8 = "Text";
         }

         switch (var8) {
            case "Decimal2":
            case "Decimal2Pct":
            case "Decimal2PL":
            case "Decimal2Pips":
               var7 = var5.getFormat("0.##");
               var4.setCellValue(Double.valueOf(var2));
               break;
            case "Decimal4":
               var7 = var5.getFormat("0.####");
               var4.setCellValue(Double.valueOf(var2));
               break;
            case "Decimal5":
               var7 = var5.getFormat("0.#####");
               var4.setCellValue(Double.valueOf(var2));
               break;
            case "Integer":
               var4.setCellValue(Integer.valueOf(var2).intValue());
               var7 = 1;
               break;
            case "Text":
            case "Time":
               var7 = 49;
               var4.setCellValue(var2);
               break;
            default:
               var4.setCellValue(var2);
         }

         if (!this.cellStyles.containsKey(var7)) {
            XSSFCellStyle var12 = var3.createCellStyle();
            var12.setDataFormat(var7);
            this.cellStyles.put(var7, var12);
         }

         CellStyle var13 = this.cellStyles.get(var7);
         if (var13 != null) {
            var4.setCellStyle(var13);
         }
      } catch (Exception var11) {
         Log.error(String.format("Error while writing cell value [%s = %s] to xls. Exc.", var6, var2), var11);
         var4.setCellValue("N/A");
      }
   }

   private String formatValue(TradelistTableColumnEntry var1, String var2, boolean var3) {
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

   private ArrayList<String> getRecordKeys(Databank var1, String var2) {
      return var2 != null && !var2.isEmpty() ? new ArrayList<>(Arrays.asList(var2.split(","))) : var1.getRecordKeys();
   }
}
