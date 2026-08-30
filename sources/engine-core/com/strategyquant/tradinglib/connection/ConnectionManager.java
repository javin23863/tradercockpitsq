package com.strategyquant.tradinglib.connection;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.IDataBuffer;
import com.strategyquant.datalib.data.impl.ConcurrentDataBuffer;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.tradinglib.order.IOrderListener;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionManager {
   public static final Logger Log = LoggerFactory.getLogger(ConnectionManager.class);
   public static final String CONNECTION_HISTORY = "History";
   public static final String STATUS_DISCONNECTED = "Disconnected";
   public static final String STATUS_CONNECTING = "Connecting";
   public static final String STATUS_CONNECTED = "Connected";
   public static final String STATUS_CONNECTION_FAILED = "Connection failed";
   private HashMap<Integer, Connection> connections = new HashMap<>();
   private HashMap<String, Integer> connectionHashCodes = new HashMap<>();
   private ArrayList<IConnectionListener> connectionListeners = new ArrayList<>();
   private ArrayList<IOrderListener> orderListeners = new ArrayList<>();
   IDataBuffer dataBuffer = null;

   public ConnectionManager() {
      this(200);
   }

   public ConnectionManager(int var1) {
      this.dataBuffer = new ConcurrentDataBuffer(var1);
   }

   public synchronized Connection createConnection(String var1, IConnectionPlugin var2, ValuesMap var3) throws Exception {
      for (String var5 : this.connectionHashCodes.keySet()) {
         if (var5.compareTo(var1) == 0) {
            throw new Exception("Cannot create connection. Connection with name \"" + var1 + "\" already exists!");
         }
      }

      Connection var7 = var2.createConnection(this, var1, var3);
      this.addConnection(var1, var7);
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var6 : this.connectionListeners) {
            var6.onConnectionAdd(var1, var2.getPluginName(), var7.getType(), 0, 0.0, 0.0, var7.getStatus());
         }
      }

      return var7;
   }

   public synchronized Connection getConnection(String var1) throws DataException {
      if (!this.connectionHashCodes.containsKey(var1)) {
         throw new DataException(1, "Connection with name '" + var1 + "' doesn't exist!");
      } else {
         return this.connections.get(this.connectionHashCodes.get(var1));
      }
   }

   public synchronized ArrayList<Connection> getAllConnections() {
      ArrayList var1 = new ArrayList();

      for (int var3 : this.connections.keySet()) {
         var1.add(this.connections.get(var3));
      }

      return var1;
   }

   public void addConnection(String var1, Connection var2) {
      int var3 = var2.getConnectionHash();
      this.connectionHashCodes.put(var1, var3);
      this.connections.put(var3, var2);
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var5 : this.connectionListeners) {
            var5.onConnectionAdd(var1, var2.getPluginName(), var2.getType(), 0, 0.0, 0.0, var2.getStatus());
         }
      }
   }

   public synchronized void editConnection(String var1, ValuesMap var2) throws Exception {
      Connection var3 = this.connections.get(this.connectionHashCodes.get(var1));
      if (var3.isConnected()) {
         var3.disconnect();
      }

      var3.setConnectionParams(var2);
      ConnectionsFileManager.saveConnections();
   }

   public synchronized void removeConnection(String var1) throws Exception {
      Connection var2 = this.connections.get(this.connectionHashCodes.get(var1));
      var2.disconnect();
      this.connections.remove(this.connectionHashCodes.get(var1));
      this.connectionHashCodes.remove(var1);
      ConnectionsFileManager.saveConnections();
      this.fireConnectionRemovedEvent(var1);
   }

   public void registerConnectionAndSymbol(ChartDef var1) throws Exception {
      if (!var1.isRegistered()) {
         String var2 = var1.getConnectionName();
         Connection var3 = this.getConnection(var2);
         IDataFeed var4 = var3.getDataFeed();
         if (var4 == null) {
            throw new Exception("Connection is not a Data Feed, cannot register symbols !");
         }

         String var5 = var1.getSymbol();
         if (!var4.checkSymbolIsSupported(var5)) {
            throw new DataException(2, "Symbol '" + var5 + "' is not supported for connection '" + var2 + "'!");
         }

         var4.registerSymbol(var5);
         var1.setRegistered();
      }
   }

   public void closeAllConnections() {
      for (int var2 : this.connections.keySet()) {
         try {
            this.connections.get(var2).disconnect();
         } catch (Exception var4) {
            Log.error("Exception thrown while closing connections.", var4);
         }
      }
   }

   public IDataBuffer getDataBuffer() {
      return this.dataBuffer;
   }

   public void registerConnectionAndSymbol() {
   }

   public synchronized boolean hasConnection(String var1) {
      return this.connectionHashCodes.containsKey(var1);
   }

   public void addConnectionListener(IConnectionListener var1) {
      this.connectionListeners.add(var1);
   }

   public void removeConnectionListener(IConnectionListener var1) {
      this.connectionListeners.remove(var1);
   }

   public void addOrderListener(IOrderListener var1) {
      this.orderListeners.add(var1);
   }

   public void removeOrderListener(IOrderListener var1) {
      this.orderListeners.remove(var1);
   }

   public void fireStateChangedEvent(String var1, String var2) {
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var4 : this.connectionListeners) {
            var4.onConnectionStateChange(var1, var2);
         }
      }
   }

   public void fireConnectionErrorEvent(String var1, Exception var2) {
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var4 : this.connectionListeners) {
            var4.onConnectionError(var1, var2);
         }
      }
   }

   public void fireConnectionRemovedEvent(String var1) {
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var3 : this.connectionListeners) {
            var3.onConnectionRemove(var1);
         }
      }
   }

   public void firePLChangedEvent(String var1, double var2) {
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var5 : this.connectionListeners) {
            var5.onConnectionPLChange(var1, var2);
         }
      }
   }

   public void fireAccountBalanceChangedEvent(String var1, double var2) {
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var5 : this.connectionListeners) {
            var5.onConnectionAccountBalanceChange(var1, var2);
         }
      }
   }

   public void fireOpenOrdersChangedEvent(String var1, int var2) {
      if (!this.connectionListeners.isEmpty()) {
         for (IConnectionListener var4 : this.connectionListeners) {
            var4.onConnectionOpenOrdersChange(var1, var2);
         }
      }
   }

   public void fireOrderAddEvent(long var1, String var3, String var4, double var5, double var7) {
      if (!this.orderListeners.isEmpty()) {
         for (IOrderListener var10 : this.orderListeners) {
            var10.onOrderAdd(var1, var3, var4, var5, var7);
         }
      }
   }

   public void fireOrderClosedEvent(long var1) {
      if (!this.orderListeners.isEmpty()) {
         for (IOrderListener var4 : this.orderListeners) {
            var4.onOrderClosed(var1);
         }
      }
   }

   public void fireOrderChangedEvent(long var1, String var3, double var4, double var6) {
      if (!this.orderListeners.isEmpty()) {
         for (IOrderListener var9 : this.orderListeners) {
            var9.onOrderChanged(var1, var3, var4, var6);
         }
      }
   }
}
