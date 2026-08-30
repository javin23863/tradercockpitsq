package com.strategyquant.tradinglib;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

public class OSUtils {
   public static void hideConsole() {
      System.out.println("Hiding console...");

      try {
         int var0 = OSUtils.Kernel32.INSTANCE.GetConsoleWindow();
         if (var0 != 0) {
            OSUtils.User32.INSTANCE.ShowWindow(var0, 0);
            System.out.println("Console window hidden.");
         } else {
            System.err.println("No console window found.");
         }
      } catch (Exception | Error var1) {
         System.err.println("Error while hiding console: " + var1.getMessage());
      }
   }

   public interface Kernel32 extends Library {
      OSUtils.Kernel32 INSTANCE = (OSUtils.Kernel32)Native.load("kernel32", OSUtils.Kernel32.class);

      int GetConsoleWindow();
   }

   public interface User32 extends Library {
      OSUtils.User32 INSTANCE = (OSUtils.User32)Native.load("user32", OSUtils.User32.class, W32APIOptions.DEFAULT_OPTIONS);

      boolean ShowWindow(int var1, int var2);
   }
}
