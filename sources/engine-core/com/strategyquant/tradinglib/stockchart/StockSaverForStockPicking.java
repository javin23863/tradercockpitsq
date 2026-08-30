package com.strategyquant.tradinglib.stockchart;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.data.LoadedPickerData;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.LoadedStockGroupData;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.talib.TALibIndicators;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.jdom2.Attribute;
import org.jdom2.JDOMException;

public class StockSaverForStockPicking {
   public static final int MAX_TRADES = 1000;
   private JarOutputStream jarOutStream;
   private OrdersList orders;
   private String path;
   private LoadedStockGroupData stockGroupData;
   private Map<String, Integer> orderCounts = new HashMap<>();
   private Map<String, List<Object[]>> tmpMapPerSymbol;

   public void save(JarOutputStream var1, LoadedPickerData var2, Stockpicker var3, OrdersList var4, String var5) throws JDOMException, Exception {
      this.jarOutStream = var1;
      this.stockGroupData = var2.stockGroupData;
      this.orders = var4;
      this.path = "Results/" + this.correctResultKey(var5);
      this.saveTimeLines(var2);
      this.saveTrades();
      this.saveMetadata(var3.TALibIndicators);
      this.saveIndicators(var3.TALibIndicators);
   }

   private void saveTimeLines(LoadedPickerData var1) throws IOException, TradingException {
      PickerDataTimeline var2 = var1.timeline;
      ByteBuffer var3 = ByteBuffer.allocate(var2.SizeD() * 8);

      for (int var4 = 0; var4 < var2.SizeD(); var4++) {
         long var5 = var2.indexToTimeD(var4);
         var3.putLong(var5);
      }

      this.createEntry("timelines.dat");
      BufferedOutputStream var7 = new BufferedOutputStream(this.jarOutStream);
      var7.write(var3.array());
      var7.flush();
      this.jarOutStream.closeEntry();
   }

   private void saveIndicators(TALibIndicators var1) throws IOException {
      for (String var3 : this.tmpMapPerSymbol.keySet()) {
         for (Object[] var6 : this.tmpMapPerSymbol.get(var3)) {
            long var7 = (Long)var6[5];
            float[][] var9 = var1.getIndicatorValue(var7);
            if (var9 != null) {
               String var10 = "indicator_" + var7 + ".dat";
               this.createEntry(var10);
               int var11 = (Integer)var6[3];
               float[] var12 = var9[var11];
               ByteBuffer var13 = ByteBuffer.allocate(var12.length * 4);

               for (int var14 = 0; var14 < var12.length; var14++) {
                  var13.putFloat(var12[var14]);
               }

               BufferedOutputStream var15 = new BufferedOutputStream(this.jarOutStream);
               var15.write(var13.array());
               var15.flush();
               this.jarOutStream.closeEntry();
            }
         }
      }
   }

   private void createEntry(String var1) throws IOException {
      JarEntry var2 = new JarEntry(this.path + "/" + var1);
      this.jarOutStream.putNextEntry(var2);
   }

   private void saveFromTo(ByteBuffer var1, long var2, long var4) throws IOException {
      var1.putLong(var2);
      var1.putLong(var4);
   }

   private float getFloat(double var1) {
      return (float)((float)var1 * 10000.0 / 10000.0);
   }

   private void saveMetadata(TALibIndicators var1) throws JDOMException, Exception {
      this.createEntry("stock_desc.csv");
      BufferedWriter var2 = new BufferedWriter(new OutputStreamWriter(this.jarOutStream, "US-ASCII"));
      LinkedList var3 = new LinkedList<>(this.stockGroupData.getSymbols());
      Collections.sort(var3);
      BlocksConfig var4 = BlocksConfig.getConfig();
      ObjectCollection var5 = var1.getIndicatorHashes().values();
      this.tmpMapPerSymbol = new HashMap<>();
      ObjectIterator var6 = var5.iterator();

      while (var6.hasNext()) {
         Object[] var7 = (Object[])var6.next();
         String var8 = (String)var7[0];
         List var9 = this.tmpMapPerSymbol.get(var8);
         if (var9 == null) {
            var9 = new LinkedList();
            this.tmpMapPerSymbol.put(var8, var9);
         }

         var9.add(var7);
      }

      for (String var19 : var3) {
         Integer var20 = this.orderCounts.get(var19);
         if (var20 == null) {
            var20 = 0;
         }

         var2.write("S;" + var19 + ";" + var20 + System.getProperty("line.separator"));
         List var21 = this.tmpMapPerSymbol.get(var19);
         if (var21 != null) {
            for (Object[] var11 : var21) {
               String var12 = (String)var11[1];
               int var13 = (Integer)var11[2];
               String var14 = (String)var11[4];
               long var15 = (Long)var11[5];
               BlockDefinition var17 = var4.getBlock("talib_" + var12);
               var2.write(
                  "I;"
                     + var15
                     + ";"
                     + var12
                     + ";"
                     + this.getIndicatorName(var17, var13, (SettingsMap)var11[6])
                     + ";"
                     + var14
                     + System.getProperty("line.separator")
               );
            }
         }
      }

      var2.flush();
      this.jarOutStream.closeEntry();
   }

   private String getIndicatorName(BlockDefinition var1, int var2, SettingsMap var3) {
      Attribute var4 = var1.getXml().getAttribute("display");
      if (var4 == null) {
         return var1.getName();
      }

      String var5 = var4.getValue();
      String[] var6 = var3.getAllKeys();

      for (String var10 : var6) {
         Object var11 = var3.get(var10);
         if (var11 == null) {
            var11 = "";
         }

         String var12 = "#" + var10 + "#";
         var5 = var5.replace(var12, var11.toString());
      }

      var5.replace("#Shift#", String.valueOf(var2));
      return var5.replace(".[]", "").replace("[]", "");
   }

   private void saveTrades() throws IOException {
      for (String var3 : this.stockGroupData.getSymbols()) {
         int var4 = this.saveTrades(var3);
         this.orderCounts.put(var3, var4);
      }
   }

   private boolean shouldShowControlOrders() {
      boolean var1 = false;

      try {
         var1 = Boolean.parseBoolean(MainApp.settings().get("showControlOrders", "false"));
      } catch (Exception var3) {
      }

      return var1;
   }

   private int saveTrades(String var1) throws IOException {
      if (this.orders != null && this.orders.size() != 0) {
         int var2 = 0;
         boolean var3 = this.shouldShowControlOrders();
         int var4 = 0;

         for (int var5 = 0; var5 < this.orders.size(); var4++) {
            char var6 = 'ꀸ';
            ByteBuffer var7 = ByteBuffer.allocate(var6);
            var7.position(16);
            int var8 = 0;
            long var9 = -1L;
            long var11 = -1L;

            while (var8 < 1000 && var5 < this.orders.size()) {
               Order var13 = this.orders.get(var5);
               var5++;
               if ((var13.Type == 1 || var13.Type == 2) && var1.equals(var13.Symbol) && (var3 || var13.isRealOrder())) {
                  byte var14 = var13.Type;
                  boolean var15;
                  if (var14 == 1) {
                     var15 = false;
                  } else {
                     var15 = true;
                  }

                  var7.putLong(var13.Ticket);
                  var7.putLong(var13.OpenTime);
                  var7.putLong(var13.CloseTime);
                  var7.putFloat(this.getFloat(var13.OpenPrice));
                  var7.putFloat(this.getFloat(var13.ClosePrice));
                  var7.putFloat(this.getFloat(var13.StopLoss));
                  var7.putFloat(this.getFloat(var13.TakeProfit));
                  var7.put((byte)(var15 ? 1 : 0));
                  if (var13.OpenTime < var9 || var9 == -1L) {
                     var9 = var13.OpenTime;
                  }

                  if (var13.CloseTime > var11 || var11 == -1L) {
                     var11 = var13.CloseTime;
                  }

                  var8++;
                  var2++;
               }
            }

            if (var11 != -1L && var9 != -1L) {
               var7.position(0);
               this.saveFromTo(var7, var9, var11);
               String var16 = var1 + "_" + var4;
               this.createEntry("trades_" + var16 + ".dat");
               BufferedOutputStream var17 = new BufferedOutputStream(this.jarOutStream);
               var17.write(var7.array());
               var17.flush();
               this.jarOutStream.closeEntry();
            }
         }

         return var2;
      } else {
         return 0;
      }
   }

   private String correctResultKey(String var1) {
      return var1.replace("/", "_LOM_");
   }
}
