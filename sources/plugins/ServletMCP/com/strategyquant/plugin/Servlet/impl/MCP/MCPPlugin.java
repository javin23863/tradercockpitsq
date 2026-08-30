package com.strategyquant.plugin.Servlet.impl.MCP;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.project.mcp.MCPTools;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "StrategyQuant")
@Name(name = "MCP Server Plugin")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "MCP server for AI tool integration")
@PluginImplementation
public class MCPPlugin implements IServletPlugin {
   private static final Logger Log = LoggerFactory.getLogger("MCPPlugin");

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      try {
         HttpServletStreamableServerTransportProvider var1 = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("").build();
         McpServer.sync(var1)
            .serverInfo("StrategyQuant X", "1.0.0")
            .capabilities(ServerCapabilities.builder().tools(false).build())
            .tools(MCPTools.createSpecs())
            .build();
         ServletContextHandler var2 = new ServletContextHandler(0);
         var2.setContextPath("/mcp");
         var2.setAllowNullPathInfo(true);
         ServletHolder var3 = new ServletHolder("mcp", var1);
         var3.setAsyncSupported(true);
         var2.addServlet(var3, "/*");
         Log.info("SQX MCP HTTP Server registered at /mcp");
         return var2;
      } catch (Exception var4) {
         Log.error("Failed to initialize MCP HTTP Server", var4);
         return null;
      }
   }
}
