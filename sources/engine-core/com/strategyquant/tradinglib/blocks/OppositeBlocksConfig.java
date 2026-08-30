package com.strategyquant.tradinglib.blocks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OppositeBlocksConfig {
   public static final Logger Log = LoggerFactory.getLogger("OppositeBlocksConfig");

   public void run(String var1) throws Exception {
      TreeMap var2 = Blocks.generateOppositeBlocksMap();
      File var3 = new File(var1);
      BufferedWriter var4 = null;

      try {
         if (!var3.exists()) {
            var3.getParentFile().mkdirs();
         }

         var4 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(var3), StandardCharsets.UTF_8));
         var4.append("Block;OppositeBlock\n");

         for (String var6 : var2.keySet()) {
            var4.append(var6);
            var4.append(";");
            var4.append((CharSequence)var2.get(var6));
            var4.append("\r\n");
         }
      } catch (Exception var15) {
      } finally {
         try {
            if (var4 != null) {
               var4.close();
            }
         } catch (IOException var14) {
         }
      }
   }

   public HashMap<String, String> load(String var1, Int2ObjectOpenHashMap<IBlock> var2) {
      HashMap var3 = new HashMap();
      File var4 = new File(var1);
      if (!var4.exists()) {
         Log.debug("File '" + var1 + "' doesn't exist!");
         return this.fillMissingBlocks(var3, var2);
      }

      ArrayList var5 = new ArrayList();

      try {
         SQUtils.fileToArrayList(var5, var1, "\r\n");
      } catch (Exception var9) {
         Log.error("Cannot read OppositeBlocks.csv file: " + var9.getMessage(), var9);
      }

      for (int var6 = 0; var6 < var5.size(); var6++) {
         String var7 = (String)var5.get(var6);
         if (!var7.contains("OppositeBlock")) {
            var7 = var7.trim();
            var7 = var7.replace("\"", "");
            String[] var8 = var7.split(";");
            if (var8.length == 2) {
               if (this.checkBlocksExistAndHaveSameParameters(var8[0], var8[1], var2)) {
                  var3.put(var8[0], var8[1]);
               } else {
                  Log.error(
                     String.format("Block '%s' or '%s' doesn't exist, or blocks don't have same parameters, using default opposite block!", var8[0], var8[1])
                  );
               }
            } else {
               Log.error("Wrong opposite block format on line " + var6 + " : " + var7);
            }
         }
      }

      return this.fillMissingBlocks(var3, var2);
   }

   private boolean checkBlocksExistAndHaveSameParameters(String var1, String var2, Int2ObjectOpenHashMap<IBlock> var3) {
      if (!var3.containsKey(var1.hashCode())) {
         return false;
      }

      if (!var3.containsKey(var2.hashCode())) {
         return false;
      }

      IBlock var4 = (IBlock)var3.get(var1.hashCode());
      IBlock var5 = (IBlock)var3.get(var2.hashCode());
      return this.blocksHaveSameParameters(var4, var5);
   }

   private boolean blocksHaveSameParameters(IBlock var1, IBlock var2) {
      Field[] var3 = var1.getClass().getFields();
      if (var3 == null) {
         return true;
      }

      for (int var4 = 0; var4 < var3.length; var4++) {
         Field var5 = var3[var4];
         if (var5.getAnnotation(Parameter.class) != null && !this.findFieldInBlock(var5, var2)) {
            return false;
         }

         if (var5.getAnnotation(Output.class) != null && !this.findFieldInBlock(var5, var2)) {
            return false;
         }
      }

      return true;
   }

   private boolean findFieldInBlock(Field var1, IBlock var2) {
      Field[] var3 = var2.getClass().getFields();
      if (var3 == null) {
         return true;
      }

      for (int var4 = 0; var4 < var3.length; var4++) {
         Field var5 = var3[var4];
         if (var1.getName().equals(var5.getName())) {
            if (!var1.getType().toString().equals(var5.getType().toString())) {
               return false;
            }

            return true;
         }
      }

      return false;
   }

   private HashMap<String, String> fillMissingBlocks(HashMap<String, String> var1, Int2ObjectOpenHashMap<IBlock> var2) {
      ObjectIterator var3 = var2.values().iterator();

      while (var3.hasNext()) {
         IBlock var4 = (IBlock)var3.next();
         String var5 = var4.getClass().getSimpleName();
         String var6 = null;
         if (!var1.containsKey(var5)) {
            OppositeBlock var7 = var4.getClass().getAnnotation(OppositeBlock.class);
            if (var7 != null) {
               var6 = var7.value();
               if (var2.containsKey(var6.hashCode())) {
                  var1.put(var5, var6);
                  continue;
               }
            }

            var1.put(var5, var5);
         }
      }

      return var1;
   }
}
