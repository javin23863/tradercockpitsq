package com.strategyquant.webguilib.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServlet extends AbstractServlet {
   private static final Logger Log = LoggerFactory.getLogger(DefaultServlet.class);

   @Override
   protected void doGet(HttpServletRequest var1, HttpServletResponse var2) throws ServletException, IOException {
      String var3 = null;
      short var4 = 200;

      try {
         HashMap var5 = new HashMap();
         this.getRequestParams(var1, var5);
         String var6 = var1.getPathInfo().substring(1);
         Log.debug("Incoming command: " + var6);
         this.dumpParams(var1.getParameterMap());
         if (var1.getParameterMap() != null && !var1.getParameterMap().isEmpty()) {
            var3 = this.execute(var6, var1.getParameterMap(), var1.getMethod());
         } else {
            var3 = this.execute(var6, var5, var1.getMethod());
         }

         if (var3 == null) {
            throw new NullPointerException("Request not processed.");
         }
      } catch (Exception var7) {
         Log.error("Exc.", var7);
         var4 = 400;
         var3 = "Server error. Reason: " + var7.getMessage();
      }

      var2.setContentType("text/html");
      var2.setStatus(var4);
      var2.getWriter().print(var3);
      var2.addHeader("Connection", "close");
      var2.flushBuffer();
   }
}
