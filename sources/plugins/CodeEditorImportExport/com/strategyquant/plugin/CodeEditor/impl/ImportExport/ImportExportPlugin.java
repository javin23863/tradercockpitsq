package com.strategyquant.plugin.CodeEditor.impl.ImportExport;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Author(name = "Tomas Brynda")
@Name(name = "ImportExport Plugin")
@Category(name = "Code editor Extension")
@License(text = "")
@ShortDesc(text = "Server API for Code Editor import/export")
@PluginImplementation
public class ImportExportPlugin implements IServletPlugin {
   private ServletContextHandler connectionContext;

   public String getProduct() {
      return "SQMANAGERSQUANTSQTRADERTESTAPPSQEDITOR";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.connectionContext == null) {
         this.connectionContext = new ServletContextHandler(1);
         this.connectionContext.setContextPath("/importexport/");
         this.connectionContext.addServlet(new ServletHolder(new ImportExportServlet()), "/*");
      }

      return this.connectionContext;
   }
}
