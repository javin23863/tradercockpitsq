package com.strategyquant.plugin.Servlet.impl.CodeEditor;

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

@Author(name = "Tamas Takacs")
@Name(name = "CodeEditor Servlet Plugin")
@Category(name = "Server Extension")
@License(text = "")
@ShortDesc(text = "CodeEditor plugin")
@PluginImplementation
public class CodeEditorPlugin implements IServletPlugin {
   private ServletContextHandler dataContext;

   public String getProduct() {
      return "SQUANTSQEDITOR";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/codeeditor/");
         this.dataContext.addServlet(new ServletHolder(new CodeEditorServlet()), "/*");
      }

      return this.dataContext;
   }
}
