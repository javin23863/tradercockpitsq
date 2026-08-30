package com.strategyquant.plugin.Connection.impl.MT4;

import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "MT4Connection - using NJ4X")
@Category(name = "Connection")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class MT4ConnectionPlugin implements IConnectionPlugin {
   public static final Logger Log = LoggerFactory.getLogger("TestConnectionPlugin");

   public String getProduct() {
      return "SQTRADERSQUANT_OLD";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      if (MainApp.checkProduct("SQTRADER")) {
         MT4TerminalManager.init();
      }
   }

   public String getPluginName() {
      return "MT4ConnectionPlugin";
   }

   public Connection createConnection(ConnectionManager var1, String var2, ValuesMap var3) throws Exception {
      return new MT4Connection(this, var1, var2, var3);
   }

   public boolean isDataFeed() {
      return true;
   }

   public boolean isExecutionEngine() {
      return true;
   }
}
