package com.strategyquant.datalib.data.imports;

import com.strategyquant.datalib.data.io.columns.AskCol;
import com.strategyquant.datalib.data.io.columns.BidCol;
import com.strategyquant.datalib.data.io.columns.CloseCol;
import com.strategyquant.datalib.data.io.columns.DateCol;
import com.strategyquant.datalib.data.io.columns.DateTimeCol;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.datalib.data.io.columns.HighCol;
import com.strategyquant.datalib.data.io.columns.LowCol;
import com.strategyquant.datalib.data.io.columns.OpenCol;
import com.strategyquant.datalib.data.io.columns.TimeCol;
import com.strategyquant.datalib.data.io.columns.UnusedCol;
import com.strategyquant.datalib.data.io.columns.VolumeCol;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvailableDataFormats {
   public static final Logger Log = LoggerFactory.getLogger(AvailableDataFormats.class);
   private static final String filePath = SQPaths.settingsDirPath + "/dataFormats.xml";
   public static final String MT5TickData = "MetaTrader5 Tick Data";
   private static AvailableDataFormats instance;
   protected ArrayList<CustomDataFormat> availableFileFormats = new ArrayList<>();

   private AvailableDataFormats() {
      this.loadPredefinedFileFormats();
      this.sortByOrderAndName(this.availableFileFormats);
      this.loadCustomFileFormats();
   }

   public ArrayList<CustomDataFormat> getAvailableFileFormats() {
      return this.availableFileFormats;
   }

   public static AvailableDataFormats getInstance() {
      if (instance == null) {
         instance = new AvailableDataFormats();
      }

      return instance;
   }

   public void registerFileFormats() {
      this.availableFileFormats.clear();
      this.loadPredefinedFileFormats();
   }

   public boolean formatExists(String var1) {
      for (CustomDataFormat var3 : this.availableFileFormats) {
         if (var3.getName().equals(var1)) {
            return true;
         }
      }

      return false;
   }

   private void loadPredefinedFileFormats() {
      CustomDataFormat var1 = new CustomDataFormat();
      var1.setName("MetaTrader4");
      var1.setSeparator(",");
      var1.setDateFormat("yyyy.MM.dd");
      var1.setSkipRows(0);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      HashMap var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new OpenCol());
      var2.put(3, new HighCol());
      var2.put(4, new LowCol());
      var2.put(5, new CloseCol());
      var2.put(6, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("MetaTrader5");
      var1.setSeparator("\t");
      var1.setDateFormat("yyyy.MM.dd");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new OpenCol());
      var2.put(3, new HighCol());
      var2.put(4, new LowCol());
      var2.put(5, new CloseCol());
      var2.put(6, new VolumeCol());
      var2.put(7, new UnusedCol());
      var2.put(8, new UnusedCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("MetaTrader5 Tick Data");
      var1.setSeparator("\t");
      var1.setDateFormat("yyyy.MM.dd");
      var1.setTimeFormat("HH:mm:ss.SSS");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var1.setOrder(100);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new BidCol());
      var2.put(3, new AskCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("SQ Tick Downloader");
      var1.setSeparator(",");
      var1.setDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateTimeCol());
      var2.put(1, new BidCol());
      var2.put(2, new AskCol());
      var2.put(3, new VolumeCol());
      var2.put(4, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("SQ3 Tick Export");
      var1.setSeparator(",");
      var1.setDateFormat("dd.MM.yyyy HH:mm:ss");
      var1.setSkipRows(0);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateTimeCol());
      var2.put(1, new AskCol());
      var2.put(2, new BidCol());
      var2.put(3, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("DukasCopy Tick Data");
      var1.setSeparator(",");
      var1.setDateFormat("dd.MM.yyyy HH:mm:ss");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateTimeCol());
      var2.put(1, new AskCol());
      var2.put(2, new BidCol());
      var2.put(3, new VolumeCol());
      var2.put(4, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("MetaTrader4 tick export");
      var1.setSeparator(",");
      var1.setDateFormat("yyyy.MM.dd HH:mm:ss");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateTimeCol());
      var2.put(1, new AskCol());
      var2.put(2, new BidCol());
      var2.put(3, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("NinjaTrader data");
      var1.setSeparator(";");
      var1.setDateFormat("yyyyMMdd");
      var1.setSkipRows(0);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new OpenCol());
      var2.put(2, new HighCol());
      var2.put(3, new LowCol());
      var2.put(4, new CloseCol());
      var2.put(5, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("NinjaTrader data (sqDataExport)");
      var1.setSeparator(";");
      var1.setDateFormat("MM/dd/yyyy HH:mm");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new OpenCol());
      var2.put(3, new HighCol());
      var2.put(4, new LowCol());
      var2.put(5, new CloseCol());
      var2.put(6, new UnusedCol());
      var2.put(7, new UnusedCol());
      var2.put(8, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("AZ-Invest Range/Renko data");
      var1.setSeparator(",");
      var1.setDateFormat("yyyy.MM.dd HH:mm:ss");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateTimeCol());
      var2.put(1, new OpenCol());
      var2.put(2, new HighCol());
      var2.put(3, new LowCol());
      var2.put(4, new CloseCol());
      var2.put(5, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("Tradestation");
      var1.setSeparator(",");
      var1.setDateFormat("MM/dd/yyyy HH:mm");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new OpenCol());
      var2.put(3, new HighCol());
      var2.put(4, new LowCol());
      var2.put(5, new CloseCol());
      var2.put(6, new VolumeCol());
      var2.put(7, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("MultiCharts");
      var1.setSeparator(",");
      var1.setDateFormat("MM/dd/yyyy");
      var1.setSkipRows(1);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new OpenCol());
      var2.put(3, new HighCol());
      var2.put(4, new LowCol());
      var2.put(5, new CloseCol());
      var2.put(6, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("Kibot daily data");
      var1.setSeparator(",");
      var1.setDateFormat("MM/dd/yyyy");
      var1.setSkipRows(0);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new OpenCol());
      var2.put(2, new HighCol());
      var2.put(3, new LowCol());
      var2.put(4, new CloseCol());
      var2.put(5, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("Kibot intraday data");
      var1.setSeparator(",");
      var1.setDateFormat("MM/dd/yyyy");
      var1.setSkipRows(0);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new OpenCol());
      var2.put(3, new HighCol());
      var2.put(4, new LowCol());
      var2.put(5, new CloseCol());
      var2.put(6, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
      var1 = new CustomDataFormat();
      var1.setName("Kibot tick data");
      var1.setSeparator(",");
      var1.setDateFormat("MM/dd/yyyy");
      var1.setSkipRows(0);
      var1.setSkipColumns(0);
      var1.setPredefined(true);
      var2 = new HashMap();
      var2.put(0, new DateCol());
      var2.put(1, new TimeCol());
      var2.put(2, new UnusedCol());
      var2.put(3, new BidCol());
      var2.put(4, new AskCol());
      var2.put(5, new VolumeCol());
      var1.setColumns(var2);
      this.availableFileFormats.add(var1);
   }

   private void loadCustomFileFormats() {
      LinkedList var1 = new LinkedList();

      try {
         File var2 = new File(filePath);
         if (!var2.exists()) {
            return;
         }

         Element var3 = XMLUtil.fileToXmlElement(var2);
         if (var3 != null) {
            for (Element var5 : var3.getChildren("CustomFormat")) {
               try {
                  CustomDataFormat var6 = new CustomDataFormat();
                  var6.setName(var5.getAttributeValue("name"));
                  var6.setSeparator(var5.getAttributeValue("separator"));
                  var6.setSkipColumns(Integer.parseInt(var5.getAttributeValue("skipColumns")));
                  var6.setSkipRows(Integer.parseInt(var5.getAttributeValue("skipRows")));
                  var6.setDateFormat(var5.getAttributeValue("dateFormat"));
                  var6.setPredefined(false);
                  Element var7 = var5.getChild("Columns");
                  if (var7 != null) {
                     HashMap var8 = new HashMap();
                     List var9 = var7.getChildren("Column");

                     for (int var10 = 0; var10 < var9.size(); var10++) {
                        String var11 = ((Element)var9.get(var10)).getText();
                        DefaultCol var12 = DataColumns.getInstance().findColTypeByName(var11);
                        if (var12 != null) {
                           var8.put(var10, var12);
                        }
                     }

                     var6.setColumns(var8);
                  }

                  var1.add(var6);
               } catch (Exception var13) {
                  Log.error("Cannot load dataformat.", var13);
               }
            }
         }
      } catch (Exception var14) {
         Log.error("Cannot load custom data formats", var14);
      }

      this.sortByOrderAndName(var1);
      this.availableFileFormats.addAll(var1);
   }

   private void sortByOrderAndName(List<CustomDataFormat> var1) {
      var1.sort(new Comparator<CustomDataFormat>() {
         public int compare(CustomDataFormat var1, CustomDataFormat var2) {
            int var3 = Integer.compare(var1.getOrder(), var2.getOrder());
            return var3 != 0 ? var3 : var1.getName().compareTo(var2.getName());
         }
      });
   }

   public void addDataFormat(CustomDataFormat var1) {
      this.availableFileFormats.add(var1);
      this.saveFormats();
   }

   public void deleteDataFormat(String var1) {
      this.remove(var1, true);
   }

   private void remove(String var1, boolean var2) {
      boolean var3 = false;

      for (CustomDataFormat var5 : this.availableFileFormats) {
         if (var5.getName().equals(var1)) {
            this.availableFileFormats.remove(var5);
            var3 = true;
            break;
         }
      }

      if (var3 && var2) {
         this.saveFormats();
      }
   }

   public void updateDataFormat(CustomDataFormat var1) {
      this.remove(var1.getName(), false);
      this.availableFileFormats.add(var1);
      this.saveFormats();
   }

   private void saveFormats() {
      Element var1 = new Element("CustomDataFormats");

      for (CustomDataFormat var3 : this.availableFileFormats) {
         if (!var3.isPredefined()) {
            Element var4 = new Element("CustomFormat");
            var4.setAttribute("name", var3.getName());
            var4.setAttribute("separator", var3.getSeparator());
            var4.setAttribute("skipColumns", String.valueOf(var3.getSkipColumns()));
            var4.setAttribute("skipRows", String.valueOf(var3.getSkipRows()));
            var4.setAttribute("dateFormat", var3.getDateFormat());
            Element var5 = new Element("Columns");

            for (int var6 = 0; var6 < var3.getColumns().size(); var6++) {
               String var7 = var3.getColumns().get(var6).getName();
               Element var8 = new Element("Column");
               var8.setText(var7);
               var5.addContent(var8);
            }

            var4.addContent(var5);
            var1.addContent(var4);
         }
      }

      try {
         File var10 = new File(filePath);
         if (!var10.exists()) {
            var10.getParentFile().mkdirs();
         }

         BufferedWriter var11 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(var10), "UTF8"));
         XMLOutputter var12 = new XMLOutputter(Format.getPrettyFormat());
         var12.output(var1, var11);
         var11.close();
      } catch (Exception var9) {
         Log.error("Saving settings failed.", var9);
      }
   }

   public CustomDataFormat findFileFormatByName(String var1) {
      for (CustomDataFormat var3 : this.availableFileFormats) {
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }
}
