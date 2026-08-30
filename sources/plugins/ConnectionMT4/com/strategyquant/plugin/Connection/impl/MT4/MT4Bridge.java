package com.strategyquant.plugin.Connection.impl.MT4;

import com.jfx.Broker;
import com.jfx.ErrUnknownSymbol;
import com.jfx.MarketInfo;
import com.jfx.SelectionPool;
import com.jfx.SelectionType;
import com.jfx.Timeframe;
import com.jfx.TradeOperation;
import com.jfx.strategy.NJ4XInvalidUserNameOrPasswordException;
import com.jfx.strategy.NJ4XMaxNumberOfTerminalsExceededException;
import com.jfx.strategy.NJ4XNoConnectionToServerException;
import com.jfx.strategy.OrderInfo;
import com.jfx.strategy.Strategy;
import com.jfx.strategy.StrategyRunner;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT4Bridge extends Strategy {
   private static final Logger Log = LoggerFactory.getLogger(MT4Bridge.class);
   private final int coordinationInterval = 200;
   private ArrayList<Long> activeOrders = new ArrayList<>();
   private ArrayList<Boolean> orderClosed = new ArrayList<>();
   private HashMap<String, Integer> activeSymbols = new HashMap<>();
   private ConnectionManager connectionManager;
   private String connectionName;
   private int connectionHash;
   private double PL;
   private double balance;
   private int openOrders;
   private String status;
   private boolean disconnectBridge = false;
   private boolean coordinationCalled = false;

   public MT4Bridge(ConnectionManager var1, String var2, int var3) {
      this.connectionManager = var1;
      this.connectionName = var2;
      this.connectionHash = var3;
      this.status = "Disconnected";
   }

   public void registerSymbol(String var1) throws Exception {
      Log.info("Registering symbol \"" + var1 + "\"...");
      if (this.isConnected()) {
         synchronized (this.activeSymbols) {
            for (String var4 : this.activeSymbols.keySet()) {
               if (var4.compareTo(var1) == 0) {
                  Log.warn("Symbol \"" + var1 + "\" is already registered");
                  return;
               }
            }

            this.activeSymbols.put(var1, var1.hashCode());
         }
      } else {
         throw new Exception("Connection is not active yet. Cannot register symbol.");
      }
   }

   public long buyNow(String var1, double var2) throws Exception {
      Log.info("Executing BUY NOW order. Symbol: \"" + var1 + "\", volume: " + var2);
      long var4 = System.currentTimeMillis() + 60000L;
      Date var6 = new Date(var4);
      return this.sendOrder(var1, TradeOperation.OP_BUY, var2, 10, 0, 0, "Buy now order", 0, var6);
   }

   public long sellNow(String var1, double var2) throws Exception {
      Log.info("Executing SELL NOW order. Symbol: \"" + var1 + "\", volume: " + var2);
      long var4 = System.currentTimeMillis() + 60000L;
      Date var6 = new Date(var4);
      return this.sendOrder(var1, TradeOperation.OP_SELL, var2, 10, 0, 0, "Sell now order", 0, var6);
   }

   public long sendOrder(String var1, TradeOperation var2, double var3, int var5, int var6, int var7, String var8, int var9, Date var10) throws Exception {
      Log.info(
         "Sending order. Symbol: \""
            + var1
            + "\", operation: "
            + var2.toString()
            + "volume: "
            + var3
            + ", slippage: "
            + var5
            + ", stop loss: "
            + var6
            + ", take profit: "
            + var7
            + ", comment: \""
            + var8
            + "\", expiration: "
            + var10
      );
      double var11 = this.marketInfo(var1, MarketInfo.MODE_POINT);
      double var15 = 0.0;
      double var17 = 0.0;
      double var13;
      if (var2 == TradeOperation.OP_BUY) {
         var13 = this.marketInfo(var1, MarketInfo.MODE_ASK);
         if (var6 > 0) {
            var15 = var13 - var6 * var11;
         }

         if (var7 > 0) {
            var17 = var13 + var7 * var11;
         }
      } else {
         var13 = this.marketInfo(var1, MarketInfo.MODE_BID);
         if (var6 > 0) {
            var15 = var13 + var6 * var11;
         }

         if (var7 > 0) {
            var17 = var13 - var7 * var11;
         }
      }

      long var19 = this.orderSend(var1, var2, var3, var13, var5, var15, var17, var8, var9, var10);
      this.activeOrders.add(var19);
      this.orderClosed.add(false);
      this.fireOrderAddEvent(var19, var2.toString(), var1, var3, var13);
      return var19;
   }

   public long sendOrder(String var1, TradeOperation var2, double var3, int var5, double var6, double var8, String var10, int var11, Date var12) throws Exception {
      Log.info(
         "Sending order. Symbol: \""
            + var1
            + "\", operation: "
            + var2.toString()
            + "volume: "
            + var3
            + ", slippage: "
            + var5
            + ", stop loss: "
            + var6
            + ", take profit: "
            + var8
            + ", comment: \""
            + var10
            + "\", expiration: "
            + var12
      );
      double var13 = this.marketInfo(var1, MarketInfo.MODE_POINT);
      double var15;
      if (var2 == TradeOperation.OP_BUY) {
         var15 = this.marketInfo(var1, MarketInfo.MODE_ASK);
      } else {
         var15 = this.marketInfo(var1, MarketInfo.MODE_BID);
      }

      long var17 = this.orderSend(var1, var2, var3, var15, var5, var6, var8, var10, var11, var12);
      this.activeOrders.add(var17);
      this.orderClosed.add(false);
      this.fireOrderAddEvent(var17, var2.toString(), var1, var3, var15);
      return var17;
   }

   public void closeOrder(long var1) throws Exception {
      Log.info("Closing order with ticket number: " + var1);
      OrderInfo var3 = this.orderGet(var1, SelectionType.SELECT_BY_TICKET, SelectionPool.MODE_TRADES);
      double var4;
      if (var3.getType() == TradeOperation.OP_BUY) {
         var4 = this.marketInfo(var3.getSymbol(), MarketInfo.MODE_BID);
      } else {
         var4 = this.marketInfo(var3.getSymbol(), MarketInfo.MODE_ASK);
      }

      this.orderClose(var1, var3.getLots(), var4, 10, 0L);
   }

   public void modifyOrder(long var1, double var3, double var5, double var7, Date var9) throws Exception {
      Log.info("Modifying order with ticket number: " + var1 + ". Stop loss: " + var5 + ", take profit: " + var7);
      this.orderModify(var1, var3, var5, var7, var9, 0L);
   }

   public void addActiveOrders() {
      Log.info("Adding active orders...");
      int var1 = this.ordersTotal();

      for (int var2 = 0; var2 < var1; var2++) {
         if (this.orderSelect(var2, SelectionType.SELECT_BY_POS, SelectionPool.MODE_TRADES)) {
            try {
               int var3 = this.orderType();
               String var4;
               if (var3 == 0) {
                  var4 = "BUY";
               } else {
                  var4 = "SELL";
               }

               Log.info(
                  String.format(
                     "Ticket no.: #%d, Type: %s, Symbol: %s, Price open: %f, Price close: %f, Profit: %f",
                     this.orderTicketNumber(),
                     var4,
                     this.orderSymbol(),
                     this.orderOpenPrice(),
                     this.orderClosePrice(),
                     this.orderProfit()
                  )
               );
               this.activeOrders.add(this.orderTicketNumber());
               this.orderClosed.add(false);
               this.fireOrderAddEvent(this.orderTicketNumber(), var4, this.orderSymbol(), this.orderLots(), this.orderOpenPrice());
            } catch (Exception var5) {
               Log.error("Error while adding order.", var5);
            }
         }
      }
   }

   public void dumpActiveOrders() throws Exception {
      Log.info("--- Active orders: --------------------------------------------------------");
      int var1 = this.ordersTotal();

      for (int var2 = 0; var2 < var1; var2++) {
         if (this.orderSelect(var2, SelectionType.SELECT_BY_POS, SelectionPool.MODE_TRADES)) {
            int var3 = this.orderType();
            String var4;
            if (var3 == 0) {
               var4 = "BUY";
            } else {
               var4 = "SELL";
            }

            Log.info(
               String.format(
                  "Ticket no.: #%d, Type: %s, Symbol: %s, Price open: %f, Price close: %f, Profit: %f",
                  this.orderTicketNumber(),
                  var4,
                  this.orderSymbol(),
                  this.orderOpenPrice(),
                  this.orderClosePrice(),
                  this.orderProfit()
               )
            );
         }
      }

      Log.info("---------------------------------------------------------------------------");
   }

   private void updateOrdersInfo() {
      int var1 = this.ordersTotal();
      boolean[] var2 = new boolean[this.activeOrders.size()];
      if (var1 != this.openOrders) {
         this.openOrders = var1;
         this.fireOpenOrdersChangedEvent();
      }

      for (int var3 = 0; var3 < var1; var3++) {
         try {
            if (this.orderSelect(var3, SelectionType.SELECT_BY_POS, SelectionPool.MODE_TRADES)) {
               for (int var4 = 0; var4 < this.activeOrders.size(); var4++) {
                  if (this.activeOrders.get(var4) == this.orderTicketNumber()) {
                     var2[var4] = true;
                     this.fireOrderChangedEvent(this.orderTicketNumber(), "active", this.orderClosePrice(), this.orderProfit());
                     break;
                  }
               }
            }
         } catch (Exception var5) {
            Log.error("Error getting order info", var5);
         }
      }

      for (int var6 = 0; var6 < var2.length; var6++) {
         if (!var2[var6] && !this.orderClosed.get(var6)) {
            this.fireOrderClosedEvent(this.activeOrders.get(var6));
            this.orderClosed.set(var6, true);
         }
      }
   }

   public void getHistoryBars(String var1, int var2) {
      for (int var3 = 0; var3 < var2; var3++) {
         try {
            double var4 = this.iHigh(var1, Timeframe.PERIOD_M1, var3);
            Log.debug("Symbol: " + var1 + ", shift: " + var3 + ", high: " + var4);
         } catch (Exception var6) {
            Log.error("Cannot load history bar no." + var3 + ".", var6);
         }
      }
   }

   public void init(String var1, int var2, StrategyRunner var3) {
      if (!this.disconnectBridge) {
         try {
            Log.info("Initializing MT4Bridge...");
            super.init(var1, var2, var3);
         } catch (ErrUnknownSymbol var5) {
            var5.printStackTrace();
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      }
   }

   public void deinit() {
      Log.info("Deinitializing MT4Bridge...");
      super.deinit();
   }

   public void coordinate() {
      try {
         IDataBuffer var1 = this.connectionManager.getDataBuffer();
         TickEvent var2 = new TickEvent();
         if (!this.coordinationCalled && !this.disconnectBridge) {
            this.coordinationCalled = true;
            this.setStatus("Connected");
         }

         synchronized (this.activeSymbols) {
            for (String var5 : this.activeSymbols.keySet()) {
               try {
                  double var6 = this.marketInfo(var5, MarketInfo.MODE_BID);
                  double var8 = this.marketInfo(var5, MarketInfo.MODE_ASK);
                  var2.setSymbolHash(this.activeSymbols.get(var5));
                  var2.setConnectionHash(this.connectionHash);
                  var2.setAsk(var8);
                  var2.setBid(var6);
                  var2.setIsSet(true);
                  var1.put(var2);
                  Log.debug(this.connectionName + " > " + var5 + " price: " + var6);
               } catch (ErrUnknownSymbol var12) {
                  Log.error("Symbol \"" + var5 + "\" not recognized. Deleting from the list of symbols...", var12);
                  this.activeSymbols.remove(var5);
               }
            }
         }

         this.updateOrdersInfo();
         double var15 = this.accountProfit();
         if (var15 != this.PL) {
            this.PL = var15;
            this.firePLChangedEvent();
         }

         double var16 = this.accountBalance();
         if (var16 != this.balance) {
            this.balance = var16;
            this.fireAccountBalanceChangedEvent();
         }
      } catch (Exception var14) {
         if (!this.disconnectBridge) {
            Log.error("Error occurred during coordination.", var14);

            try {
               this.disconnect();
               this.status = "Disconnected";
               this.fireStateChangedEvent();
            } catch (Exception var11) {
               Log.error("Unable to remove terminal \"" + this.connectionName + "\"");
            }

            this.fireConnectionErrorEvent(var14);
         }
      }
   }

   public synchronized void connect(String var1, int var2, Broker var3, String var4, String var5, String var6, boolean var7) throws NJ4XInvalidUserNameOrPasswordException, NJ4XMaxNumberOfTerminalsExceededException, NJ4XNoConnectionToServerException, IOException {
      super.connect(var1, var2, var3, var4, var5, var6, var7);
      this.disconnectBridge = false;
      this.coordinationCalled = false;
   }

   public void disconnectBridge() {
      try {
         this.disconnect();
      } catch (IOException var2) {
         Log.error("Error during disconnecting.", var2);
      }

      this.disconnectBridge = true;
   }

   public void disconnect() throws IOException {
      super.disconnect();
      this.coordinationCalled = false;
      this.status = "Disconnected";
      this.fireStateChangedEvent();
   }

   public long coordinationIntervalMillis() {
      return 200L;
   }

   public boolean isConnected() {
      return !this.disconnectBridge && super.isConnected() && this.coordinationCalled;
   }

   public boolean isDisconnectedByUser() {
      return this.disconnectBridge;
   }

   private void firePLChangedEvent() {
      this.connectionManager.firePLChangedEvent(this.connectionName, this.PL);
   }

   private void fireAccountBalanceChangedEvent() {
      this.connectionManager.fireAccountBalanceChangedEvent(this.connectionName, this.balance);
   }

   private void fireOpenOrdersChangedEvent() {
      this.connectionManager.fireOpenOrdersChangedEvent(this.connectionName, this.openOrders);
   }

   private void fireOrderAddEvent(long var1, String var3, String var4, double var5, double var7) {
      this.connectionManager.fireOrderAddEvent(var1, var3, var4, var5, var7);
   }

   private void fireOrderClosedEvent(long var1) {
      this.connectionManager.fireOrderClosedEvent(var1);
   }

   private void fireOrderChangedEvent(long var1, String var3, double var4, double var6) {
      this.connectionManager.fireOrderChangedEvent(var1, var3, var4, var6);
   }

   private void fireStateChangedEvent() {
      this.connectionManager.fireStateChangedEvent(this.connectionName, this.status);
   }

   private void fireConnectionErrorEvent(Exception var1) {
      this.connectionManager.fireConnectionErrorEvent(this.connectionName, var1);
   }

   public String getConnectionState() {
      return this.status;
   }

   public int getOpenOrdersCount() {
      return this.openOrders;
   }

   public double getTotalPL() {
      return this.PL;
   }

   public double getAccountBalance() {
      return this.balance;
   }

   public String getStatus() {
      return this.status;
   }

   public void setStatus(String var1) {
      if (this.status.compareTo(var1) != 0) {
         this.status = var1;
         this.fireStateChangedEvent();
      }
   }
}
