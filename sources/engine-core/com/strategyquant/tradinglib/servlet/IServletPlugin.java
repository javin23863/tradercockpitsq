package com.strategyquant.tradinglib.servlet;

import com.strategyquant.pluginlib.ISQPlugin;
import org.eclipse.jetty.server.Handler;

public interface IServletPlugin extends ISQPlugin {
   Handler getHandler();
}
