package com.strategyquant.plugin.EquityChart.impl.Volume;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.lib.SQTime;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.HideDatabankChoice;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.SubChart;
import com.strategyquant.tradinglib.equitychart.SubChartDataset;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import com.strategyquant.tradinglib.results.SymbolInfo;
import it.unimi.dsi.fastutil.longs.Long2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Equity Chart Volume plugin")
@Category(name = "EquityChart")
@License(text = "")
@ShortDesc(text = "Short description")
@HideDatabankChoice
@PluginImplementation
public class EquityChartVolume implements IEquityChartAddon {
   private static byte Auto = 5;
   private static byte Size = 2;
   private static byte Money = 1;
   private static byte Off = 0;

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 1;
   }

   public void initPlugin() throws Exception {
   }

   public void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, Map<String, String[]> var5, String var6) throws Exception {
      byte var7 = Byte.parseByte(((String[])var5.get("volume"))[0]);
      if (var7 != Off) {
         byte var8 = this.getVolumeType(var2, var7);
         String var9 = var8 == Size ? "Volume" : "Volume ($)";
         double var11 = -11111.0;
         boolean var13 = false;
         SubChartDataset var10;
         if (var6.equals("time")) {
            Long2DoubleAVLTreeMap var19 = new Long2DoubleAVLTreeMap();

            for (int var20 = 0; var20 < var4.size(); var20++) {
               Order var14 = var4.get(var20);
               double var15 = this.getVolume(var2, var14, var7);
               long var17 = SQTime.getDateInMs(var14.CloseTime);
               if (var19.containsKey(var17)) {
                  var19.put(var17, var19.get(var17) + var15);
               } else {
                  var19.put(var17, var15);
               }
            }

            var10 = new SubChartDataset(var9, var19.size());
            ObjectBidirectionalIterator var28 = var19.long2DoubleEntrySet().iterator();

            while (var28.hasNext()) {
               Entry var21 = (Entry)var28.next();
               double var23 = var21.getDoubleValue();
               long var25 = var21.getLongKey();
               if (var11 == -11111.0) {
                  var11 = var23;
               } else if (var11 != var23) {
                  var13 = true;
               }

               var10.addValue(var25, var23);
            }
         } else {
            var10 = new SubChartDataset(var9, var4.size());

            for (int var26 = 0; var26 < var4.size(); var26++) {
               Order var22 = var4.get(var26);
               double var24 = this.getVolume(var2, var22, var7);
               if (var11 == -11111.0) {
                  var11 = var24;
               } else if (var11 != var24) {
                  var13 = true;
               }

               var10.addValue(var26, var24);
            }
         }

         if (var7 != Auto || var7 == Auto && var13) {
            var10.d3Format();
            var10.setName(var9);
            var10.type = "bar";
            var10.color = "#61a8ff";
            SubChart var27 = new SubChart(var9);
            var27.forceStartYFromZero = true;
            var27.name = var9;
            var27.titlePosition = "chartLeftTop";
            var27.addDataset(var10);
            var1.addSubChart(var27);
         }
      }
   }

   private double getVolume(ResultsGroup var1, Order var2, byte var3) throws Exception {
      byte var4 = this.getVolumeType(var1, var3);
      if (var4 == Size) {
         return var2.Size;
      }

      InstrumentInfo var5 = this.getInstrumentInfo(var1, var2);
      return var2.Size * var2.OpenPrice * var5.pointValue;
   }

   private byte getVolumeType(ResultsGroup var1, byte var2) {
      if (var2 == Auto) {
         return var1.isStockpickerStrategy() ? Money : Size;
      } else {
         return var2;
      }
   }

   private InstrumentInfo getInstrumentInfo(ResultsGroup var1, Order var2) throws Exception {
      InstrumentInfo var3 = null;
      SymbolInfo var4 = var1.symbols().get(var2.Symbol);
      if (var4 != null) {
         var3 = var4.InstrumentInfo();
      }

      if (var3 == null) {
         var3 = var1.symbols().getDefaultInstrumentInfo();
      }

      if (var3 == null) {
         throw new Exception(String.format("Instrument info not found for %s / %s", var1.getName(), var2.Symbol));
      } else {
         return var3;
      }
   }

   public String getName() {
      return "Volume";
   }
}
