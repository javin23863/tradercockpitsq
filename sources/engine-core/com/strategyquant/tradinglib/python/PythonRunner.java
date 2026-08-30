package com.strategyquant.tradinglib.python;

import com.strategyquant.lib.app.MainApp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonRunner {
   public static final Logger Log = LoggerFactory.getLogger(PythonRunner.class);
   private String pythonExePath;
   private File scriptFolder;
   private File tmpFolder;
   private Process process;
   private Object lock = new Object();
   private volatile boolean stopped;

   public PythonRunner() {
      File var1 = new File(MainApp.getDataPath(), "internal");
      File var2 = new File(var1, "python");
      File var3 = new File(var2, "runtime");
      this.pythonExePath = new File(var3, "python.exe").getAbsolutePath();
      this.scriptFolder = new File(var2, "scripts");
      this.tmpFolder = new File(var1, "tmp");
   }

   public File getTmpFile(String var1, String var2) {
      File var3;
      do {
         String var4 = var1 + UUID.randomUUID().toString().replace("-", "") + var2;
         var3 = new File(this.tmpFolder, var4);
      } while (var3.exists());

      return var3;
   }

   private Process createPythonProcess(List<String> var1) throws IOException {
      this.stopped = false;
      ProcessBuilder var2 = new ProcessBuilder(var1);
      var2.environment().remove("PYTHONPATH");
      var2.environment().put("PYTHONNOUSERSITE", "1");
      var2.redirectErrorStream(true);
      return var2.start();
   }

   private List<String> getPythonCommands(File var1, String... var2) {
      ArrayList var3 = new ArrayList();
      var3.add(this.pythonExePath);
      var3.add(var1.getAbsolutePath());
      var3.addAll(Arrays.asList(var2));
      Log.info("Running {} {}", var1.getName(), String.join(" ", var2));
      return var3;
   }

   private String readOutput(Process var1) throws IOException {
      StringBuilder var2 = new StringBuilder();
      BufferedReader var3 = new BufferedReader(new InputStreamReader(var1.getInputStream()));

      String var4;
      try {
         while ((var4 = var3.readLine()) != null) {
            var2.append(var4).append("\n");
         }
      } catch (Throwable var7) {
         try {
            var3.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      var3.close();
      return var2.toString();
   }

   public String run(String var1, PythonRunner.ProgressCallback var2, String... var3) throws IOException, InterruptedException {
      File var4 = new File(this.scriptFolder, var1);
      List var5 = this.getPythonCommands(var4, var3);
      this.process = this.createPythonProcess(var5);
      String var6 = this.listenPythonOutput(var2, this.process);
      int var7 = this.process.waitFor();
      synchronized (this.lock) {
         this.process = null;
      }

      if (this.stopped) {
         return null;
      }

      switch (var7) {
         case 0:
            return var6;
         case 1:
            Log.error("Error while running script: " + var6);
            throw new RuntimeException("Error while running script: " + var6);
         case 2:
            throw new FileNotFoundException("Script not found: " + var4.getAbsolutePath());
         default:
            throw new RuntimeException("Unexpected error (exit code " + var7 + "):\n" + var6);
      }
   }

   private String listenPythonOutput(PythonRunner.ProgressCallback var1, Process var2) throws InterruptedException {
      StringBuilder var3 = new StringBuilder();
      Thread var4 = new Thread(() -> {
         try {
            BufferedReader var4x = new BufferedReader(new InputStreamReader(var2.getInputStream()));

            String var5;
            try {
               while ((var5 = var4x.readLine()) != null) {
                  var3.append(var5);
                  if (var1 != null) {
                     this.callCallback(var1, var5);
                  } else {
                     Log.debug(var5);
                  }
               }
            } catch (Throwable var8) {
               try {
                  var4x.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            var4x.close();
         } catch (IOException var9) {
            Log.error("Error reading output", var9);
         }
      });
      var4.start();
      var4.join();
      return var3.toString();
   }

   private void callCallback(PythonRunner.ProgressCallback var1, String var2) {
      var2 = var2.trim();
      if (var2.startsWith("SQERROR:")) {
         var1.onError(var2.substring(8).trim());
      } else if (var2.startsWith("SQRESULT:")) {
         var1.onResult(var2.substring(9).trim());
      } else if (var2.startsWith("SQMESSAGE:")) {
         var1.onMessage(var2.substring(10).trim());
      } else if (var2.startsWith("SQPROGRESS:")) {
         String var3 = var2.substring(11).trim();

         try {
            double var4 = Double.valueOf(var3);
            var1.onProgress(var4);
         } catch (Exception var6) {
            Log.error("Error while parsing progress response: " + var3, var6);
         }
      } else {
         Log.debug(var2);
      }
   }

   public void cancel() {
      synchronized (this.lock) {
         if (this.process != null) {
            this.stopped = true;
            this.process.destroyForcibly();
         }
      }
   }

   public interface ProgressCallback {
      void onResult(String var1);

      void onProgress(double var1);

      void onError(String var1);

      void onMessage(String var1);
   }

   public static class ProgressCallbackAdapter implements PythonRunner.ProgressCallback {
      @Override
      public void onResult(String var1) {
      }

      @Override
      public void onProgress(double var1) {
      }

      @Override
      public void onError(String var1) {
      }

      @Override
      public void onMessage(String var1) {
      }
   }
}
