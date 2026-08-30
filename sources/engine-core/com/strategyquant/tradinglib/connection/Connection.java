package com.strategyquant.tradinglib.connection;

import com.strategyquant.lib.ValuesMap;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import org.json.JSONObject;

public abstract class Connection {
   protected final ConnectionManager connectionManager;
   protected final String connectionName;
   protected final IConnectionPlugin connectionPlugin;
   protected final int hashCode;
   private IDataFeed dataFeed;
   private IExecutionEngine executionEngine;
   protected ValuesMap connParams;
   private String connectionType;

   public Connection(ConnectionManager var1, String var2, IConnectionPlugin var3, ValuesMap var4) {
      this.connectionManager = var1;
      this.connectionName = var2;
      this.connectionPlugin = var3;
      this.hashCode = var2.hashCode();
      this.connParams = var4;
      this.initializeEngines();
      if (this.dataFeed == null && this.executionEngine == null) {
         throw new IllegalArgumentException("Connection must implement at least one of the interfaces: DataFeed or ExecutionEngine !");
      }

      this.connectionType = null;
      if (this.dataFeed != null) {
         this.connectionType = "DataFeed";
      }

      if (this.executionEngine != null) {
         this.connectionType = this.connectionType + (this.connectionType != null ? " & " : "") + "Execution";
      }
   }

   public String toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("ConnectionName", this.connectionName);
      var1.put("ConnectionPlugin", this.connectionPlugin.getPluginName());
      var1.put("ConnectionParams", this.connParams.toJSON());
      return var1.toString();
   }

   protected void setExecutionEngine(IExecutionEngine var1) {
      this.executionEngine = var1;
   }

   protected void setDataFeed(IDataFeed var1) {
      this.dataFeed = var1;
   }

   public String getConnectionName() {
      return this.connectionName;
   }

   public int getConnectionHash() {
      return this.hashCode;
   }

   public String getPluginName() {
      return this.connectionPlugin.getPluginName();
   }

   public ValuesMap getConnectionParams() {
      return this.connParams;
   }

   public void setConnectionParams(ValuesMap var1) {
      this.connParams = var1;
   }

   public IExecutionEngine getExecutionEngine() {
      return this.executionEngine;
   }

   public boolean isExecutionEngine() {
      return this.executionEngine != null;
   }

   public IDataFeed getDataFeed() {
      return this.dataFeed;
   }

   public boolean isDataFeed() {
      return this.dataFeed != null;
   }

   public String getType() {
      return this.connectionType;
   }

   public abstract void initializeEngines();

   public abstract String getStatus();

   public abstract void connect() throws Exception;

   public abstract boolean isConnected();

   public abstract void disconnect() throws Exception;
}
