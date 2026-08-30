package com.strategyquant.plugin.EquityChart.impl.Volatility;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.HideDatabankChoice;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.charts.linechart.series.XYValue;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.SubChart;
import com.strategyquant.tradinglib.equitychart.SubChartDataset;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import com.strategyquant.tradinglib.results.SpecialValues;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Volatility plugin")
@Category(name = "EquityChart")
@License(text = "")
@ShortDesc(text = "Short description")
@HideDatabankChoice
@PluginImplementation
public class Volatility implements IEquityChartAddon {
   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 4;
   }

   public void initPlugin() throws Exception {
   }

   public void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, Map<String, String[]> var5, String var6) throws Exception {
      if (Boolean.parseBoolean(((String[])var5.get("volatility"))[0])) {
         if (var6.equals("time")) {
            String var7 = null;
            if (var2.subResult(var3).containsKey(SpecialValues.Symbol)) {
               var7 = var2.subResult(var3).getString(SpecialValues.Symbol);
            } else {
               var7 = var2.specialValues().getString(SpecialValues.Symbol, null);
            }

            if (var7 != null && !var7.equals("Portfolio") && !DataManager.isGroupAlias(var7)) {
               if (var7.contains(",")) {
                  var7 = var7.split(",")[0];
               }

               String var8 = var7 + ", ATR(14)";
               SubChart var9 = new SubChart("Volatility");
               var9.name = var8;
               var9.titlePosition = "chartLeftTop";
               var1.addSubChart(var9);

               try {
                  double var10 = 2.5;
                  String var12 = "No Session";
                  long[] var13 = EquityChartUtils.dateRange(var4);
                  ChartDef var14 = new ChartDef("History", var7, "D1", var13[0], var13[1], var10, var12);
                  IDataLoader var15 = DataManager.getDataLoader(var14, 1, null);
                  var15.open();
                  double var16 = -1.0;
                  double var18 = 0.0;
                  int var20 = 0;
                  ObjectArrayList var21 = new ObjectArrayList();

                  while (var15.hasNextTick()) {
                     VersatileData var22 = new VersatileData();
                     var15.getNextTick(var22);
                     double var23 = var22.high - var22.low;
                     if (var16 < 0.0) {
                        var16 = var23;
                        var18 = var22.close;
                     } else {
                        var23 = Math.max(Math.abs(var22.low - var18), Math.max(var23, Math.abs(var22.high - var18)));
                        var16 = ((Math.min(var20 + 1, 14) - 1) * var16 + var23) / Math.min(var20 + 1, 14);
                        var18 = var22.close;
                     }

                     var20++;
                     var21.add(new XYValue(var22.time, var16));
                  }

                  var15.close();
                  SubChartDataset var28 = new SubChartDataset("Volatility", var21.size());
                  var28.decimals = 6;
                  var28.setName(var8);
                  var28.type = "line";
                  var28.color = "#000000";

                  for (int var30 = 0; var30 < var21.size(); var30++) {
                     var28.addValue(Long.valueOf(((XYValue)var21.get(var30)).x), Double.valueOf(((XYValue)var21.get(var30)).y));
                  }

                  var9.addDataset(var28);
               } catch (DataException var25) {
                  var9.name = var9.name + " - " + String.format("No data found for symbol '%s'", var7);
                  throw var25;
               } catch (Exception var26) {
                  var9.name = var9.name + " - Error: " + var26.getMessage();
                  throw var26;
               }
            }
         }
      }
   }

   public String getName() {
      return "Volatility";
   }
}
