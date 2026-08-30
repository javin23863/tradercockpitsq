package com.strategyquant.plugin.Databank.impl.Rename;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.servlet.IServletPlugin;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Databank rename")
@Category(name = "DatabankActions")
@License(text = "")
@ShortDesc(text = "Renames selected strategies in databank")
@PluginImplementation
public class DatabankRenamePlugin implements ISQPlugin, IServletPlugin {
	
	/*
	 * The log
	 */
	public static final Logger Log = LoggerFactory.getLogger(DatabankRenamePlugin.class);
	
	/*
	 * The context path
	 */
	private ServletContextHandler dataContext;
	
	/*
	 * The servlet
	 */
	private DatabankRenameServlet servlet;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/*
	 * Registers DatabankRename servlet under context path /databank
	 */
	@Override
	public Handler getHandler() {
		if(dataContext == null){
			dataContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
	        dataContext.setContextPath("/databank/");
	        dataContext.addServlet(new ServletHolder(servlet),"/*");
		}
		
        return dataContext;
	}
	
	//------------------------------------------------------------------------

	@Override
	public String getProduct() {
		return "SQUANT";
	}

	//------------------------------------------------------------------------
	
	@Override
	public int getPreferredPosition() {
		return 0;
	}

	//------------------------------------------------------------------------
	
	/*
	 * Creates DatabankRename servlet
	 */
	@Override
	public void initPlugin() throws Exception {
		this.servlet = new DatabankRenameServlet();
	}
}