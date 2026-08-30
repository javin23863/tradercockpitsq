package com.strategyquant.plugin.EquityChart.impl.DailyChart;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
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
@Name(name = "Daily chart plugin")
@Category(name = "EquityChart")
@License(text = "")
@ShortDesc(text = "Short description")
@HideDatabankChoice
@PluginImplementation
public class DailyChart implements IEquityChartAddon {
   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 3;
   }

   public void initPlugin() throws Exception {
   }

   public void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, Map<String, String[]> var5, String var6) throws Exception {
      if (Boolean.parseBoolean(((String[])var5.get("dailychart"))[0])) {
         if (var6.equals("time")) {
            String var7 = null;
            if (var2.subResult(var3).containsKey(SpecialValues.Symbol)) {
               var7 = var2.subResult(var3).getString(SpecialValues.Symbol);
            } else {
               var7 = var2.specialValues().getString(SpecialValues.Symbol, var7);
            }

            if (var7 != null && !var7.equals("Portfolio") && !DataManager.isGroupAlias(var7)) {
               if (var7.contains(",")) {
                  var7 = var7.split(",")[0];
               }

               String var8 = var7 + ", Daily";
               SubChart var9 = new SubChart("DailyChart");
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
                  ObjectArrayList var16 = new ObjectArrayList();

                  while (var15.hasNextTick()) {
                     VersatileData var17 = new VersatileData();
                     var15.getNextTick(var17);
                     var16.add(new XYValue(var17.time, var17.close));
                  }

                  var15.close();
                  SubChartDataset var22 = new SubChartDataset("DailyChart", var16.size());
                  var22.setName(var8);
                  var22.type = "line";
                  var22.color = "#000000";

                  for (int var18 = 0; var18 < var16.size(); var18++) {
                     var22.addValue(Long.valueOf(((XYValue)var16.get(var18)).x), Double.valueOf(((XYValue)var16.get(var18)).y));
                  }

                  var9.addDataset(var22);
               } catch (DataException var19) {
                  var9.name = var9.name + " - " + L.t("No data found for symbol '%s'", new Object[]{var7});
                  throw var19;
               } catch (Exception var20) {
                  var9.name = var9.name + " - " + L.t("Error: %s", new Object[]{var20.getMessage()});
                  throw var20;
               }
            }
         }
      }
   }

   public String getName() {
      return "Daily chart";
   }
}
