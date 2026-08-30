package com.strategyquant.plugin.Saver.impl.StrategyTrades;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.file.ISaverPlugin;
import com.strategyquant.tradinglib.tradelist.TradelistExport;
import com.strategyquant.tradinglib.tradelist.TradelistTableView;
import com.strategyquant.tradinglib.tradelist.TradelistViewsManager;
import java.util.ArrayList;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "SQ3SaverPlugin plugin")
@Category(name = "File Saver")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class StrategyTradesSaverPlugin implements ISaverPlugin, IProgram {
   public static final Logger Log = LoggerFactory.getLogger(StrategyTradesSaverPlugin.class);
   public static final String[] extensions = new String[]{"csv", "xlsx"};

   public String getProduct() {
      return "any";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      Program.register("SaverStrategyTrades", this);
   }

   public String[] getFileExtensions() {
      return extensions;
   }

   public Object call(String var1, Object... var2) throws Exception {
      if (var1.equals("save")) {
         this.save((ResultsGroup)var2[0], (String)var2[1], (Map<String, String[]>)var2[2]);
      }

      return null;
   }

   public void save(ResultsGroup var1, String var2, Map<String, String[]> var3) throws Exception {
      try {
         boolean var5 = Boolean.parseBoolean(((String[])var3.get("useComma"))[0]);
         String var6 = ((String[])var3.get("data"))[0];
         OrdersList var4;
         if (var6.equals("main")) {
            if (this.shouldShowControlOrders()) {
               var4 = var1.orders().filter(var1.getMainResultKey(), (byte)0, (byte)127, false);
            } else {
               var4 = var1.orders().filterExcludingControlOrders(var1.getMainResultKey(), (byte)0, (byte)127, false);
            }
         } else {
            var4 = var1.orders();
         }

         TradelistTableView var7 = TradelistViewsManager.getInstance().getSelectedTradelistView();
         if (var7 == null) {
            Log.error("Cannot load the list of trades. No tradelist view is set.");
         }

         ArrayList var8 = var7.getData(var4, 0, var4.size(), true);
         TradelistExport var9 = new TradelistExport();
         if (var2.endsWith("xlsx")) {
            var9.toXlsx(var8, var2, var5);
         } else {
            var9.toCsv(var8, var2, var5);
         }
      } catch (Exception var10) {
         Log.error("Failed to export trades of strategy '" + var1.getName() + "'. Exc: ", var10);
         throw new Exception("Failed to export trades of strategy '" + var1.getName() + "'. Reason: " + var10.getMessage());
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

   public ISaverPlugin clone() {
      return new StrategyTradesSaverPlugin();
   }
}
