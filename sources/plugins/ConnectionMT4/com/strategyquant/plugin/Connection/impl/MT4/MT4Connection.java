package com.strategyquant.plugin.Connection.impl.MT4;

import com.jfx.TradeOperation;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import java.util.ArrayList;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT4Connection extends Connection {
   public static final Logger Log = LoggerFactory.getLogger("MT4Connection");
   private MT4Bridge mt4Bridge;
   private Thread connStateCheckThread;
   private boolean endConnStateCheck = false;

   public MT4Connection(IConnectionPlugin var1, ConnectionManager var2, String var3, ValuesMap var4) throws Exception {
      super(var2, var3, var1, var4);
      this.checkConnection(var2, var4);
   }

   public void initializeEngines() {
      this.mt4Bridge = new MT4Bridge(this.connectionManager, this.connectionName, this.getConnectionHash());
      this.setDataFeed(new MT4DataFeed(this, this.mt4Bridge));
      this.setExecutionEngine(new MT4ExecutionEngine(this, this.mt4Bridge));
   }

   private void checkConnection(ConnectionManager var1, ValuesMap var2) throws Exception {
      ArrayList var3 = var1.getAllConnections();
      Object var4 = var2.get("username");
      Object var5 = var2.get("brokerIP");
      if (var4 == null) {
         throw new Exception("Cannot create connection. Parameter \"username\" not found!");
      }

      if (var5 == null) {
         throw new Exception("Cannot create connection. Parameter \"brokerIP\" not found!");
      }

      for (Connection var7 : var3) {
         Object var8 = var7.getConnectionParams().get("username");
         Object var9 = var7.getConnectionParams().get("brokerIP");
         if (var8 != null && var9 != null && var8.toString().compareTo(var4.toString()) == 0 && var9.toString().compareTo(var5.toString()) == 0) {
            throw new Exception("Cannot create connection. Connection to user account \"" + var8 + "\" and broker IP " + var5 + " already exists!");
         }
      }
   }

   public void connect() throws Exception {
      if (!this.mt4Bridge.isConnected() && this.mt4Bridge.getStatus().compareTo("Disconnected") == 0) {
         String var1 = this.connParams.get("username").toString();
         String var2 = this.connParams.get("password").toString();
         String var3 = this.connParams.get("brokerIP").toString();
         boolean var4 = false;
         if (this.connParams.get("hide") != null) {
            var4 = Boolean.parseBoolean(this.connParams.get("hide").toString());
         }

         boolean var5 = false;
         if (this.connParams.get("minimize") != null) {
            var5 = Boolean.parseBoolean(this.connParams.get("minimize").toString());
         }

         boolean var6 = false;
         if (this.connParams.get("connectOnStart") != null) {
            var6 = Boolean.parseBoolean(this.connParams.get("connectOnStart").toString());
         }

         this.mt4Bridge.setStatus("Connecting");

         try {
            MT4TerminalManager.createTerminal(var1, var2, var3, this.connectionName, this.mt4Bridge, var4, var5, var6);
         } catch (Exception var8) {
            this.mt4Bridge.setStatus("Connection failed");
            throw var8;
         }

         this.startConnectionStateCheck();
      } else {
         Log.error("Cannot connect. Connection \"" + this.connectionName + "\" is already active.");
      }
   }

   private void startConnectionStateCheck() {
      if (this.connStateCheckThread != null && this.connStateCheckThread.isAlive()) {
         this.endConnStateCheck = true;
         this.connStateCheckThread.interrupt();

         try {
            this.connStateCheckThread.join();
         } catch (InterruptedException var2) {
            var2.printStackTrace();
         }

         this.endConnStateCheck = false;
      }

      this.connStateCheckThread = new Thread() {
         @Override
         public void run() {
            try {
               sleep(20000L);
            } catch (InterruptedException var2) {
               var2.printStackTrace();
            }

            if (!MT4Connection.this.endConnStateCheck) {
               if (!MT4Connection.this.mt4Bridge.isConnected() && MT4Connection.this.mt4Bridge.getStatus().compareTo("Disconnected") != 0) {
                  MT4Connection.this.mt4Bridge.setStatus("Connection failed");
               }
            }
         }
      };
      this.connStateCheckThread.start();
   }

   public void disconnect() throws Exception {
      Log.info("Disconnecting MT4 Connection \"" + this.connectionName + "\"");
      this.mt4Bridge.disconnectBridge();
      this.mt4Bridge.setStatus("Disconnected");
      Object var1 = this.connParams.get("connectOnStart");
      if (var1 != null && Boolean.parseBoolean(var1.toString())) {
         MT4TerminalManager.killTerminalProcess(this.connectionName);
      } else {
         MT4TerminalManager.removeTerminal(this.connectionName);
      }
   }

   public boolean isConnected() {
      return this.mt4Bridge.isConnected();
   }

   public String getStatus() {
      return this.mt4Bridge.getConnectionState();
   }

   public long buyNow(String var1, double var2) throws Exception {
      return this.mt4Bridge.buyNow(var1, var2);
   }

   public long sellNow(String var1, double var2) throws Exception {
      return this.mt4Bridge.sellNow(var1, var2);
   }

   public long sendOrder(String var1, String var2, double var3, int var5, int var6, int var7, String var8, int var9, Date var10) throws Exception {
      if (var2.compareTo("BUY") == 0) {
         return this.mt4Bridge.sendOrder(var1, TradeOperation.OP_BUY, var3, var5, var6, var7, var8, var9, var10);
      } else if (var2.compareTo("SELL") == 0) {
         return this.mt4Bridge.sendOrder(var1, TradeOperation.OP_SELL, var3, var5, var6, var7, var8, var9, var10);
      } else {
         throw new Exception("Unrecognized operation \"" + var2 + "\". Expected \"BUY\" or \"SELL\".");
      }
   }

   public long sendOrder(String var1, String var2, double var3, int var5, double var6, double var8, String var10, int var11, Date var12) throws Exception {
      if (var2.compareTo("BUY") == 0) {
         return this.mt4Bridge.sendOrder(var1, TradeOperation.OP_BUY, var3, var5, var6, var8, var10, var11, var12);
      } else if (var2.compareTo("SELL") == 0) {
         return this.mt4Bridge.sendOrder(var1, TradeOperation.OP_SELL, var3, var5, var6, var8, var10, var11, var12);
      } else {
         throw new Exception("Unrecognized operation \"" + var2 + "\". Expected \"BUY\" or \"SELL\".");
      }
   }

   public void modifyOrder(long var1, double var3, double var5, double var7, Date var9) throws Exception {
      this.mt4Bridge.modifyOrder(var1, var3, var5, var7, var9);
   }

   public void closeOrder(long var1) throws Exception {
      this.mt4Bridge.closeOrder(var1);
   }

   public String getConnectionName() {
      return this.connectionName;
   }
}
