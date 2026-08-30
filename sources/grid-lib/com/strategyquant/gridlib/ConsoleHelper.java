package com.strategyquant.gridlib;

public class ConsoleHelper {
   public void run(ConsoleHelper.KeyHandler var1) throws Exception {
      char var2;
      while ((var2 = (char)System.in.read()) != 'q') {
         if (var1 != null) {
            var1.handle(var2);
         }
      }
   }

   public interface KeyHandler {
      void handle(char var1) throws Exception;
   }
}
