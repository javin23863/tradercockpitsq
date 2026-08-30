package com.strategyquant.tradinglib.project.mcp;

import com.strategyquant.pluginlib.program.Program;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPTools {
   private static final Logger Log = LoggerFactory.getLogger("MCPTools");

   public static List<SyncToolSpecification> createSpecs() {
      JsonSchema var0 = new JsonSchema("object", Collections.emptyMap(), null, null, null, null);
      JsonSchema var1 = new JsonSchema("object", Map.of("name", Map.of("type", "string", "description", "Project name")), List.of("name"), null, null, null);
      JsonSchema var2 = new JsonSchema(
         "object",
         Map.of("name", Map.of("type", "string", "description", "Project name"), "databank", Map.of("type", "string", "description", "Databank name")),
         List.of("name", "databank"),
         null,
         null,
         null
      );
      JsonSchema var3 = new JsonSchema(
         "object",
         Map.of(
            "name",
            Map.of("type", "string", "description", "Project name"),
            "databank",
            Map.of("type", "string", "description", "Databank name"),
            "strategy",
            Map.of("type", "string", "description", "Strategy name")
         ),
         List.of("name", "databank", "strategy"),
         null,
         null,
         null
      );
      return List.of(spec("list_projects", "List all available projects in StrategyQuant X", var0, (var0x, var1x) -> {
         try {
            HashMap var2x = new HashMap();
            String var3x = (String)Program.get("Project").call("list", new Object[]{var2x});
            return result(var3x);
         } catch (Exception var4) {
            return error(var4);
         }
      }), spec("list_databanks", "List all databanks in a project", var1, (var0x, var1x) -> {
         try {
            String var2x = (String)var1x.arguments().get("name");
            HashMap var3x = new HashMap();
            var3x.put("projectName", new String[]{var2x});
            String var4 = (String)Program.get("Project").call("databankList", new Object[]{var3x});
            return result(var4);
         } catch (Exception var5) {
            return error(var5);
         }
      }), spec("list_strategies", "List strategies in a databank of a project", var2, (var0x, var1x) -> {
         try {
            String var2x = (String)var1x.arguments().get("name");
            String var3x = (String)var1x.arguments().get("databank");
            HashMap var4 = new HashMap();
            var4.put("projectName", new String[]{var2x});
            var4.put("databankName", new String[]{var3x});
            String var5 = (String)Program.get("Project").call("listStrategies", new Object[]{var4});
            return result(var5);
         } catch (Exception var6) {
            return error(var6);
         }
      }), spec("get_strategy_stats", "Get statistics columns and values for a specific strategy in a databank", var3, (var0x, var1x) -> {
         try {
            String var2x = (String)var1x.arguments().get("name");
            String var3x = (String)var1x.arguments().get("databank");
            String var4 = (String)var1x.arguments().get("strategy");
            HashMap var5 = new HashMap();
            var5.put("projectName", new String[]{var2x});
            var5.put("databankName", new String[]{var3x});
            var5.put("strategyName", new String[]{var4});
            String var6 = (String)Program.get("Project").call("getStrategyStats", new Object[]{var5});
            return result(var6);
         } catch (Exception var7) {
            return error(var7);
         }
      }), spec("run_project", "Start a project in StrategyQuant X", var1, (var0x, var1x) -> {
         try {
            String var2x = (String)var1x.arguments().get("name");
            HashMap var3x = new HashMap();
            var3x.put("projectName", new String[]{var2x});
            String var4 = (String)Program.get("Project").call("start", new Object[]{var3x});
            return result(var4);
         } catch (Exception var5) {
            return error(var5);
         }
      }), spec("stop_project", "Stop a running project in StrategyQuant X", var1, (var0x, var1x) -> {
         try {
            String var2x = (String)var1x.arguments().get("name");
            HashMap var3x = new HashMap();
            var3x.put("projectName", new String[]{var2x});
            String var4 = (String)Program.get("Project").call("stop", new Object[]{var3x});
            return result(var4);
         } catch (Exception var5) {
            return error(var5);
         }
      }));
   }

   private static SyncToolSpecification spec(String var0, String var1, JsonSchema var2, BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> var3) {
      return SyncToolSpecification.builder().tool(Tool.builder().name(var0).description(var1).inputSchema(var2).build()).callHandler(var3::apply).build();
   }

   private static CallToolResult result(String var0) {
      return CallToolResult.builder().content(List.of(new TextContent(extractMessage(var0)))).isError(false).build();
   }

   private static CallToolResult error(Exception var0) {
      Log.error("MCP tool error", var0);
      return CallToolResult.builder().content(List.of(new TextContent("Error: " + var0.getMessage()))).isError(true).build();
   }

   static String extractMessage(String var0) {
      return var0;
   }
}
