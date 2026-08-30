package com.strategyquant.tradinglib.project.console;

import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.customData.CustomDataTypes;
import com.strategyquant.lib.L;
import com.strategyquant.lib.app.impl.MainAppStandardImpl;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.mcp.MCPTools;
import com.strategyquant.tradinglib.strategy.SourceCode;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

public class SQConsole extends ConsoleImpl {
   private List<String> initSyncList = new ArrayList<>();

   public SQConsole(MainAppStandardImpl var1) {
      super("StrategyQuant X", "sqcli");
   }

   public SQConsole(String var1, String var2) {
      super(var1, var2);
   }

   @Override
   public void runCommand(String[] var1) throws Exception {
      this.parsedParams = this.parseParams(var1);
      switch (var1[0]) {
         case "-project":
            this.project();
            break;
         case "-databank":
            this.databank();
            break;
         case "-tools":
            this.tools();
            break;
         case "-stockgroup":
            this.stockgroups();
            break;
         case "-stockgroups":
            this.stockgroups();
            break;
         case "-sourcecode":
            this.sourcecode();
            break;
         case "-extindicators":
            this.extIndicators();
            break;
         case "-mcp":
            this.runMcpStdioServer();
            break;
         case "quit":
         case "exit":
         case "-exit":
            this.exit();
            break;
         default:
            super.runCommand(var1);
      }
   }

   @Override
   public void exit() {
      try {
         CLILogger.log(L.t("Synchronizing databanks to files", new Object[0]), true);
         String var1 = (String)Program.get("Project").call("synchronizeAllDatabanks", null);
         CLILogger.log(this.parseCallResponse(var1));
      } catch (Exception var2) {
         CLILogger.log(L.t("Cannot synchronize databanks. Reason: %s", new Object[]{var2.getMessage()}));
      }

      super.exit();
   }

   private void stockgroups() throws Exception {
      String var1 = this.getParam("action");
      String var2 = var1;
      HashMap var3 = new HashMap();
      switch (var1) {
         case "list":
            CLILogger.log(L.t("List of available groups", new Object[0]), true);
            break;
         case "importstocks":
            var3.put("basketId", new String[]{this.getParam("id")});
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "import";
            break;
         case "exportstocks":
            var3.put("basketId", new String[]{this.getParam("id")});
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "export";
            break;
         case "import":
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "loadXml";
            break;
         case "export":
            var3.put("ids[]", new String[]{this.getParam("id")});
            var3.put("filePath", new String[]{this.getParam("filepath")});
            var2 = "saveXml";
            break;
         case "updateAll":
            var2 = "updateData";
            BasketOfStocksManager.getInstance().sync(null);
            List var6 = BasketOfStocksManager.getInstance().getGroups();
            List var7 = var6.stream()
               .filter(var0 -> !var0.getName().equalsIgnoreCase("[[S&P 100 limited]]"))
               .map(var0 -> var0.getId().toString())
               .collect(Collectors.toList());
            var3.put("basketId", new String[]{String.join(",", var7)});
            break;
         case "update":
            var2 = "updateData";
            var3.put("basketId", new String[]{this.getParam("id")});
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }

      String var8 = (String)Program.get("GroupOfStocks").call(var2, new Object[]{var3});
      CLILogger.log(var1.equals("list") ? this.parseListGroups(var8) : this.parseCallResponse(var8));
   }

   private String parseListGroups(String var1) {
      try {
         JSONObject var2 = new JSONObject(var1);
         if (var2.has("success")) {
            JSONArray var3 = (JSONArray)var2.get("rows");
            StringBuilder var4 = new StringBuilder();
            var4.append("ID\tName\n");

            for (int var5 = 0; var5 < var3.length(); var5++) {
               JSONObject var6 = (JSONObject)var3.get(var5);
               JSONArray var7 = (JSONArray)var6.get("data");
               JSONObject var8 = (JSONObject)var6.get("userdata");
               var4.append(var8.getInt("id") + "\t" + var7.get(0) + "\n");
            }

            return var4.toString();
         }

         if (var2.has("error")) {
            return var2.getString("error") + "\n" + "--------------------------------------------------";
         }
      } catch (Exception var9) {
      }

      return "";
   }

   private void tools() throws Exception {
      String var1 = this.getParam("action");
      String var2 = this.getParam("file");
      String var3 = this.getParam("output", null);
      String var4 = this.getParam("usecomma", "false");
      String var5 = this.getParam("data", "all");
      switch (var1) {
         case "orderstocsv":
            CLILogger.log(L.t("Extracting strategy orders to csv", new Object[0]), true);
            this.saveOrders(var2, var3, "csv", var4, var5);
            break;
         case "orderstoxlsx":
            CLILogger.log(L.t("Extracting strategy orders to xlsx", new Object[0]), true);
            this.saveOrders(var2, var3, "xlsx", var4, var5);
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   private void saveOrders(String var1, String var2, String var3, String var4, String var5) throws Exception {
      HashMap var6 = new HashMap();
      var6.put("useComma", new String[]{var4});
      var6.put("data", new String[]{var5});
      File var7 = new File(var1);
      if (!var7.exists()) {
         throw new Exception(L.t("File '%s' doesn't exist.", new Object[]{var7.getAbsolutePath()}));
      }

      File var8 = var2 != null ? new File(var2) : null;
      IProgram var9 = Program.get("Loader");
      IProgram var10 = Program.get("SaverStrategyTrades");
      if (var7.isDirectory()) {
         if (var8 != null) {
            if (!var8.exists()) {
               var8.mkdirs();
            }

            if (!var8.isDirectory()) {
               throw new Exception(L.t("Output file '%s' is not a directory.", new Object[]{var8.getAbsolutePath()}));
            }
         }

         File[] var11 = var7.listFiles();
         if (var11 != null) {
            for (File var15 : var11) {
               ResultsGroup var16 = (ResultsGroup)var9.call("loadFile", new Object[]{var15.getAbsolutePath()});
               String var17 = var16.getName() + "." + var3;
               if (var8 != null) {
                  var17 = var8.getAbsolutePath() + "/" + var17;
               } else if (var7.getParentFile() != null) {
                  var17 = var7.getParentFile().getAbsolutePath() + "/" + var17;
               } else {
                  var17 = var7.getAbsolutePath() + "/" + var17;
               }

               try {
                  var10.call("save", new Object[]{var16, var17, var6});
                  CLILogger.log(L.t("%s - orders exported to '%s'", new Object[]{var16.getName(), new File(var17).getAbsolutePath()}));
               } catch (Exception var20) {
                  CLILogger.log(L.t("Cannot export orders of strategy  '%s'", new Object[]{var7.getAbsolutePath()}));
               }
            }
         }
      } else {
         if (var8 != null && !var8.exists() && var8.getParentFile() != null) {
            var8.getParentFile().mkdirs();
         }

         ResultsGroup var21 = (ResultsGroup)var9.call("loadFile", new Object[]{var7.getAbsolutePath()});
         String var22 = var21.getName() + "." + var3;
         if (var8 != null) {
            var22 = var8.getAbsolutePath();
         } else if (var7.getParentFile() != null) {
            var22 = var7.getParentFile().getAbsolutePath() + "/" + var22;
         }

         if (!var22.endsWith("." + var3)) {
            var22 = var22 + "." + var3;
         }

         try {
            var10.call("save", new Object[]{var21, var22, var6});
            CLILogger.log(L.t("%s - orders exported to '%s'", new Object[]{var21.getName(), new File(var22).getAbsolutePath()}));
         } catch (Exception var19) {
            CLILogger.log(L.t("Cannot export orders of strategy  '%s'", new Object[]{var7.getAbsolutePath()}));
         }
      }
   }

   protected void project() throws Exception {
      HashMap var1 = new HashMap();
      String var2 = this.getParam("action");
      String var3 = null;
      if (!var2.equals("list")) {
         var3 = this.getParam("name");
         var1.put("projectName", new String[]{var3});
      }

      String var4 = "Project";
      String var5;
      switch (var2) {
         case "list":
            CLILogger.log(L.t("List of available projects", new Object[0]), true);
            var5 = "list";
            break;
         case "start":
            CLILogger.log(L.t("Starting project '%s'", new Object[]{this.getParam("name")}), true);
            var5 = "start";
            this.syncDatabanks(var1);
            break;
         case "startOnlyTask":
            try {
               Integer.valueOf(this.getParam("task"));
            } catch (Exception var10) {
               throw new Exception(L.t("Task must be a number.", new Object[0]));
            }

            var1.put("action", new String[]{"runTask"});
            var1.put("taskName", new String[]{this.getParam("task")});
            CLILogger.log(L.t("Starting project '%s' task %s only", new Object[]{this.getParam("name"), this.getParam("task")}), true);
            var5 = "start";
            this.syncDatabanks(var1);
            break;
         case "startFromTask":
            try {
               Integer.valueOf(this.getParam("task"));
            } catch (Exception var9) {
               throw new Exception(L.t("Task must be a number.", new Object[0]));
            }

            var1.put("action", new String[]{"runFrom"});
            var1.put("taskName", new String[]{this.getParam("task")});
            CLILogger.log(L.t("Starting project '%s' from task %s", new Object[]{this.getParam("name"), this.getParam("task")}), true);
            var5 = "start";
            this.syncDatabanks(var1);
            break;
         case "stop":
            CLILogger.log(L.t("Stopping project %s", new Object[]{this.getParam("name")}), true);
            var5 = "stop";
            break;
         case "pause":
            CLILogger.log(L.t("Pausing project %s", new Object[]{this.getParam("name")}), true);
            var5 = "pause";
            break;
         case "resume":
            CLILogger.log(L.t("Resuming project %s", new Object[]{this.getParam("name")}), true);
            var5 = "resume";
            break;
         case "remove":
            CLILogger.log(L.t("Removing project %s", new Object[]{this.getParam("name")}), true);
            var4 = "TaskManager";
            var5 = "removeProject";
            break;
         case "status":
            CLILogger.log(L.t("Status of project %s", new Object[]{this.getParam("name")}), true);
            var5 = "status";
            this.syncDatabanks(var1);
            break;
         case "loadconfig":
            var1.put("file", new String[]{this.getParam("file")});
            CLILogger.log(L.t("Loading config of project %s", new Object[]{var3}), true);
            var4 = "Project";
            var5 = "loadConfig";
            if (var3.equalsIgnoreCase("Builder")) {
               var1.put("projectName", new String[]{"Builder"});
            } else if (var3.equalsIgnoreCase("Retester")) {
               var1.put("projectName", new String[]{"Retester"});
            } else if (var3.equalsIgnoreCase("Optimizer")) {
               var1.put("projectName", new String[]{"Optimizer"});
            } else {
               var4 = "TaskManager";
               var5 = "openProject";
               var1.put("loadAsIs", new String[]{"true"});
            }
            break;
         case "saveconfig":
            var1.put("file", new String[]{this.getParam("file")});
            CLILogger.log(L.t("Saving config of project %s", new Object[]{var3}), true);
            var4 = "Project";
            var5 = "saveConfig";
            if (var3.equalsIgnoreCase("Builder")) {
               var1.put("projectName", new String[]{"Builder"});
            } else if (var3.equalsIgnoreCase("Retester")) {
               var1.put("projectName", new String[]{"Retester"});
            } else if (var3.equalsIgnoreCase("Optimizer")) {
               var1.put("projectName", new String[]{"Optimizer"});
            } else {
               var4 = "TaskManager";
               var5 = "saveProject";
            }
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var2}));
      }

      String var11 = (String)Program.get(var4).call(var5, new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var11));
   }

   protected void databank() throws Exception {
      HashMap var1 = new HashMap();
      var1.put("projectName", new String[]{this.getParam("project")});
      String var2 = this.getParam("action");
      if (!var2.equals("list")) {
         var1.put("databankName", new String[]{this.getParam("name")});
      }

      String var3 = "Project";
      String var4 = null;
      String var5 = null;
      switch (var2) {
         case "list":
            CLILogger.log(L.t("List of available databanks", new Object[0]), true);
            var4 = "databankList";
            this.syncDatabanks(var1);
            break;
         case "count":
            CLILogger.log(L.t("Number of strategies", new Object[0]), true);
            var4 = "databankCount";
            this.syncDatabanks(var1);
            break;
         case "load":
            CLILogger.log(L.t("Loading reports", new Object[0]), true);
            var1.put("folder", new String[]{this.getParam("folder")});
            var1.put("strategies", new String[]{this.getParam("strategies", "all")});
            var4 = "loadFilesToDatabank";
            break;
         case "save":
            CLILogger.log(L.t("Saving reports", new Object[0]), true);
            var1.put("folder", new String[]{this.getParam("folder")});
            var1.put("strategies", new String[]{this.getParam("strategies", "all")});
            var4 = "saveReports";
            break;
         case "delete":
            CLILogger.log(L.t("Deleting selected reports", new Object[0]), true);
            var1.put("strategies", new String[]{this.getParam("strategies")});
            var4 = "removeReports";
            break;
         case "clear":
            CLILogger.log(L.t("Clearing databank", new Object[0]), true);
            var4 = "removeAllReports";
            this.syncDatabanks(var1);
            break;
         case "copy":
            CLILogger.log(L.t("Copying reports", new Object[0]), true);
            var5 = (String)Program.get("Project").call("copyStrategies", new Object[]{var1});
            CLILogger.log(this.parseCallResponse(var5));
            var1.put("projectName", new String[]{this.getParam("destproject")});
            var1.put("databankName", new String[]{this.getParam("destdatabank")});
            var4 = "pasteStrategies";
            break;
         case "move":
            CLILogger.log(L.t("Moving reports", new Object[0]), true);
            var5 = (String)Program.get("Project").call("cutStrategies", new Object[]{var1});
            CLILogger.log(this.parseCallResponse(var5));
            var1.put("projectName", new String[]{this.getParam("destproject")});
            var1.put("databankName", new String[]{this.getParam("destdatabank")});
            var4 = "pasteStrategies";
            break;
         case "synctofiles":
            CLILogger.log(L.t("Syncing datatabank to files", new Object[0]), true);
            var4 = "synchronizeDatabank";
            break;
         case "syncfromfiles":
            CLILogger.log(L.t("Syncing databank from files", new Object[0]), true);
            var4 = "syncfromfiles";
            break;
         case "create":
            CLILogger.log(L.t("Creating datatabank", new Object[0]), true);
            var4 = "createDatabank";
            String var8 = this.getParam("position", null);
            if (var8 != null) {
               var1.put("position", new String[]{var8});
            }
            break;
         case "remove":
            CLILogger.log(L.t("Removing datatabank", new Object[0]), true);
            var4 = "removeDatabank";
            break;
         case "export":
            CLILogger.log(L.t("Exporting databank contents", new Object[0]), true);
            var1.put("path", new String[]{this.getParam("file")});
            var1.put("view", new String[]{this.getParam("view", "Default - Main data")});
            var3 = "ResultsDatabankActions";
            var4 = "exportDatabankToCSV";
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var2}));
      }

      var5 = (String)Program.get(var3).call(var4, new Object[]{var1});
      CLILogger.log(this.parseCallResponse(var5));
   }

   @Override
   protected void addSymbol() throws Exception {
      String var1 = this.getParam("datasource", "dukascopy");
      if (var1.equalsIgnoreCase("sqequity")) {
         if (!this.getParam("confirm").equalsIgnoreCase("yes")) {
            throw new Exception(L.t("Please read and agree to StrategyQuant Data Usage Conditions", new Object[0]));
         }

         HashMap var2 = new HashMap();
         var2.put("symbols", new String[]{this.getParam("symbols")});
         var2.put("postfix", new String[]{this.getParam("postfix", "")});
         var2.put("barType", new String[]{this.getBarType()});
         String var3 = (String)Program.get("DataSourceSQEquityData").call("add", new Object[]{var2});
         CLILogger.log(this.parseCallResponse(var3));
      } else if (var1.equalsIgnoreCase("sqfutures")) {
         if (!this.getParam("confirm").equalsIgnoreCase("yes")) {
            throw new Exception(L.t("Please read and agree to StrategyQuant Data Usage Conditions", new Object[0]));
         }

         HashMap var4 = new HashMap();
         var4.put("symbols", new String[]{this.getParam("symbols")});
         var4.put("postfix", new String[]{this.getParam("postfix", "")});
         var4.put("barType", new String[]{this.getBarType()});
         String var5 = (String)Program.get("DataSourceSQFuturesData").call("add", new Object[]{var4});
         CLILogger.log(this.parseCallResponse(var5));
      } else {
         super.addSymbol();
      }
   }

   private void syncDatabanks(Map<String, String[]> var1) throws Exception {
      if (var1.containsKey("projectName")) {
         String var2 = ((String[])var1.get("projectName"))[0];
         if (!this.initSyncList.contains(var2)) {
            this.initSyncList.add(var2);
            CLILogger.log(L.t("Syncing databank(s) from files", new Object[0]));
            String var3 = (String)Program.get("Project").call("synchronizeDatabanks", new Object[]{var1});
            CLILogger.log(this.parseCallResponse(var3));
         }
      }
   }

   private void sourcecode() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "generate":
            String var4 = this.getParam("engine");
            String var5 = this.getParam("s");
            String var6 = this.getParam("o");
            SourceCode var7 = new SourceCode();
            var7.generate(var4, var5, var6);
            return;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }

   private void runMcpStdioServer() {
      try {
         CLILogger.log(L.t("Starting MCP stdio server...", new Object[0]), true);
         PipedOutputStream var1 = new PipedOutputStream();
         PipedInputStream var2 = new PipedInputStream(var1, 65536);
         StdioServerTransportProvider var3 = new StdioServerTransportProvider(McpJsonDefaults.getMapper(), var2, System.out);
         McpServer.sync(var3)
            .serverInfo("StrategyQuant X", "1.0.0")
            .capabilities(ServerCapabilities.builder().tools(false).build())
            .tools(MCPTools.createSpecs())
            .build();
         byte[] var4 = new byte[4096];

         int var5;
         while ((var5 = System.in.read(var4)) != -1) {
            var1.write(var4, 0, var5);
            var1.flush();
         }
      } catch (Exception var6) {
         CLILogger.log(L.t("MCP server error: %s", new Object[]{var6.getMessage()}));
      }
   }

   private void extIndicators() throws Exception {
      String var1 = this.getParam("action");
      switch (var1) {
         case "list":
            CLILogger.log(L.t("List of external indicators", new Object[0]), true);
            String var16 = (String)Program.get("DataManagerCustomData").call("list", null);
            CLILogger.log(this.parseCallResponse(var16));
            break;
         case "add":
            CLILogger.log(L.t("Add external indicator", new Object[0]), true);
            String var5 = this.getParam("values");
            String[] var6 = var5.split(",");
            if (var6.length == 0) {
               throw new Exception(L.t("At least one value must be set.", new Object[0]));
            }

            JSONArray var7 = new JSONArray();

            for (String var26 : var6) {
               JSONObject var12 = new JSONObject();
               var12.put("name", var26);
               var12.put("mt4", "");
               var12.put("mt5", "");
               var12.put("el", "");
               var7.put(var12);
            }

            byte var17 = Byte.parseByte(this.getParam("type"));
            if (!CustomDataTypes.availableDataTypes().contains(var17)) {
               throw new Exception(L.t("Unrecognized data type %s.", new Object[]{var17}));
            }

            HashMap var21 = new HashMap();
            var21.put("name", new String[]{this.getParam("name")});
            var21.put("values", new String[]{var7.toString()});
            var21.put("type", new String[]{var17 + ""});
            String var15 = (String)Program.get("DataManagerCustomData").call("add", new Object[]{var21});
            CLILogger.log(this.parseCallResponse(var15));
            break;
         case "import":
            CLILogger.log(L.t("Import data from file", new Object[0]), true);
            String var22 = this.getParam("name");
            String var25 = this.getParam("file");
            HashMap var19 = new HashMap();
            var19.put("name", new String[]{var22});
            var19.put("file", new String[]{var25});
            String var14 = (String)Program.get("DataManagerCustomData").call("importcli", new Object[]{var19});
            CLILogger.log(this.parseCallResponse(var14));
            break;
         case "load":
            CLILogger.log(L.t("Load indicator from file.", new Object[0]), true);
            String var24 = this.getParam("filepath");
            HashMap var18 = new HashMap();
            var18.put("filePath", new String[]{var24});
            String var13 = (String)Program.get("DataManagerCustomData").call("load", new Object[]{var18});
            CLILogger.log(this.parseCallResponse(var13));
            break;
         case "save":
            CLILogger.log(L.t("Save indicator to file.", new Object[0]), true);
            String var11 = this.getParam("filepath");
            String var10 = this.getParam("name");
            HashMap var9 = new HashMap();
            var9.put("filePath", new String[]{var11});
            var9.put("data", new String[]{var10});
            String var4 = (String)Program.get("DataManagerCustomData").call("save", new Object[]{var9});
            CLILogger.log(this.parseCallResponse(var4));
            break;
         default:
            throw new Exception(L.t("Unrecognized action %s.", new Object[]{var1}));
      }
   }
}
