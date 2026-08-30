package com.strategyquant.plugin.EquityChart.impl.Drawdown;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.HideDatabankChoice;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.SubChart;
import com.strategyquant.tradinglib.equitychart.SubChartDataset;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import it.unimi.dsi.fastutil.longs.Long2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Equity Chart DD plugin")
@Category(name = "EquityChart")
@License(text = "")
@ShortDesc(text = "Short description")
@HideDatabankChoice
@PluginImplementation
public class EquityChartDrawdown implements IEquityChartAddon {
   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 2;
   }

   public void initPlugin() throws Exception {
   }

   public void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, Map<String, String[]> var5, String var6) throws Exception {
      byte var7 = Byte.parseByte(((String[])var5.get("drawdown"))[0]);
      if (var7 != 0) {
         SubChart var8 = new SubChart("Drawdown");
         var8.name = "Drawdown (" + PlTypes.print(var7) + ")";
         var8.titlePosition = "chartLeftTop";
         if (var7 == 111) {
            var8.hideYTitle = true;
            var7 = 1;
         }

         SubChartDataset var9 = new SubChartDataset("Drawdown", var4.size());
         double var11 = 0.0;
         Result var15 = var2.subResult(var3);
         Long2FloatRBTreeMap var16 = var15.getMaxDailyDD();
         Long2FloatRBTreeMap var17 = var15.getMaxDailyDDPct();
         Long2DoubleAVLTreeMap var18 = new Long2DoubleAVLTreeMap();
         SettingsMap var19 = var15.getSettings();
         double var20 = var19.getDouble("MoneyManagement.InitialCapital", 10000.0);
         double var22 = var20;
         double var24 = var20;
         boolean var26 = var16 != null && !var16.isEmpty();
         boolean var27 = var17 != null && !var17.isEmpty();
         if (var6.equals("time") && var7 == 40 && var26) {
            ObjectBidirectionalIterator var41 = var16.long2FloatEntrySet().iterator();

            while (var41.hasNext()) {
               Entry var43 = (Entry)var41.next();
               long var37 = var43.getLongKey();
               var11 = var43.getFloatValue();
               var18.put(var37, var11);
            }
         } else if (var6.equals("time") && var7 == 50 && var27) {
            ObjectBidirectionalIterator var40 = var17.long2FloatEntrySet().iterator();

            while (var40.hasNext()) {
               Entry var29 = (Entry)var40.next();
               long var36 = var29.getLongKey();
               var11 = var29.getFloatValue();
               var18.put(var36, var11);
            }
         } else {
            double var28 = 0.0;

            for (int var30 = 0; var30 < var4.size(); var30++) {
               Order var10 = var4.get(var30);
               if (var7 == 30) {
                  var24 += var10.PipsPL;
                  var11 = specialSubtraction(var22, var24);
               } else if (var7 == 20) {
                  var24 += var10.PL;
                  var11 = specialSubtraction(var22, var24);
                  var11 = this.getPercentageDD(var11, var22);
               } else if (var7 == 40) {
                  var28 = var24 - var10.MAE;
                  var24 += var10.PL;
                  var11 = specialSubtraction(var22, var28);
                  if (var24 > var22) {
                     var22 = var24;
                  }
               } else if (var7 == 50) {
                  var28 = var24 - var10.MAE;
                  var24 += var10.PL;
                  var11 = specialSubtraction(var22, var28);
                  var11 = this.getPercentageDD(var11, var22);
                  if (var24 > var22) {
                     var22 = var24;
                  }
               } else {
                  var24 += var10.PL;
                  var11 = specialSubtraction(var22, var24);
               }

               if (var6.equals("time")) {
                  long var13 = SQTime.getDateInMs(var10.CloseTime);
                  if (!var18.containsKey(var13) || var18.get(var13) > var11) {
                     var18.put(var13, var11);
                  }
               } else {
                  var9.addValue(var30, var11);
               }

               if (var24 > var22) {
                  var22 = var24;
               }
            }
         }

         if (var6.equals("time")) {
            var9 = new SubChartDataset("Drawdown", var18.size());
            ObjectBidirectionalIterator var42 = var18.long2DoubleEntrySet().iterator();

            while (var42.hasNext()) {
               it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry var44 = (it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry)var42.next();
               var9.addValue(var44.getLongKey(), var44.getDoubleValue());
            }
         }

         var9.setName("Drawdown");
         var9.type = "bar";
         var9.color = "#ff6961";
         var8.addDataset(var9);
         var1.addSubChart(var8);
      }
   }

   private Long2DoubleAVLTreeMap generateDailyEquityMap(OrdersList var1) {
      Long2DoubleAVLTreeMap var2 = new Long2DoubleAVLTreeMap();
      long[] var3 = EquityChartUtils.dateRange(var1);
      long var4 = var3[0];
      long var6 = var3[1];
      if (var1.isEmpty()) {
         return var2;
      }

      for (int var11 = 0; var11 < var1.size(); var11++) {
         Order var10 = var1.get(var11);
         long var8 = SQTime.getDateInMs(var10.CloseTime);
         if (var2.containsKey(var8)) {
            var2.put(var8, var2.get(var8) + var10.PL);
         } else {
            var2.put(var8, var10.PL);
         }
      }

      long var13 = SQTime.getDateInMs(var4);
      double var14 = 0.0;

      do {
         if (var2.containsKey(var13)) {
            var14 += var2.get(var13);
         } else {
            var2.put(var13, var14);
         }

         var13 = SQTime.addDays(var13, 1);
      } while (var13 <= var6);

      return var2;
   }

   private static double specialSubtraction(double var0, double var2) {
      double var4 = var0 - var2;
      if (var4 < 0.0) {
         var4 = 0.0;
      }

      return -1.0 * var4;
   }

   private double getPercentageDD(double var1, double var3) {
      return !(var3 <= 0.0) && !(var1 > var3) ? var1 / (var3 / 100.0) : -1.0;
   }

   public String getName() {
      return "Drawdown";
   }
}
