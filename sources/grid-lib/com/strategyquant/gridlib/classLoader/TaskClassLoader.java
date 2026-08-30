package com.strategyquant.gridlib.classLoader;

import com.strategyquant.gridlib.compute.performer.JmsJobInfo;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskClassLoader {
   private static final Logger LOGGER = LoggerFactory.getLogger(TaskClassLoader.class);
   private ClassLoader classLoader = TaskClassLoader.class.getClassLoader();

   public void loadJarsAndClasses(String... var1) {
      List var2 = this.getUrls(var1);
      URL[] var3 = var2.toArray(new URL[0]);
      LOGGER.debug("Registering JARs: " + var2);
      this.classLoader = new URLClassLoader(var3, TaskClassLoader.class.getClassLoader());
   }

   private void addFileUrl(List<URL> var1, File var2) {
      try {
         var1.add(var2.toURI().toURL());
      } catch (MalformedURLException var4) {
         LOGGER.error("", var4);
      }
   }

   private List<URL> getUrl(File var1) {
      LinkedList var2 = new LinkedList();
      File[] var3 = var1.listFiles();
      if (var3 == null) {
         return var2;
      }

      for (File var7 : var3) {
         if (var7.isDirectory()) {
            var2.addAll(this.getUrl(var7));
         } else if (var7.getName().toLowerCase().endsWith(".jar")) {
            this.addFileUrl(var2, var7);
         }
      }

      return var2;
   }

   private List<URL> getUrls(String... var1) {
      LinkedList var2 = new LinkedList();

      for (String var6 : var1) {
         var2.addAll(this.getUrl(new File(var6)));
      }

      return var2;
   }

   public <T> T createInstance(String var1, JmsJobInfo var2) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
      Class var3 = this.classLoader.loadClass(var2.getClassName());
      Constructor var4 = var3.getDeclaredConstructor(String.class, int.class, Map.class);
      return (T)var4.newInstance(var1, var2.getFlag(), var2.getParams());
   }
}
