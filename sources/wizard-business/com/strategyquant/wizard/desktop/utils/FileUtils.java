package com.strategyquant.wizard.desktop.utils;

import java.io.File;

public class FileUtils {
   public static String getAppFolder() {
      return getRootFolder() + "\\internal\\web\\SQWIZARD\\";
   }

   public static String getRootFolder() {
      File var0 = new File("");
      return var0.getAbsolutePath();
   }

   public static String getCodeFolder() {
      return getRootFolder() + "\\extend\\code\\";
   }
}
