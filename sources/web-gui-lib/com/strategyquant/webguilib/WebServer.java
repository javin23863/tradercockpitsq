package com.strategyquant.webguilib;

public interface WebServer {
   void regenerateFiles() throws Exception;

   String getWebPath();

   String getPluginsPath();

   int getPortFrom();

   int getPortTo();

   int getUsedPort();

   void start() throws Exception;

   WebAppManager getWebAppManager();

   void showErrorDialog(String var1, String var2);

   String getBaseURL();
}
