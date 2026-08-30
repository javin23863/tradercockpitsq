package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategiesSaver {
   public static final Logger Log = LoggerFactory.getLogger("StrategiesSaver");
   private static final String LOCK_STRATEGIESSAVER = "StrategiesSaver";
   private static boolean stopped = false;

   public static void saveAll(boolean var0, String var1) {
      List var2 = ProjectEngine.list();
      int var3 = 0;
      stopped = false;

      for (int var4 = 0; var4 < var2.size() && !stopped; var4++) {
         SQProject var5 = (SQProject)var2.get(var4);
         ArrayList var6 = var5.getDatabanks().list();

         for (int var7 = 0; var7 < var6.size() && !stopped; var7++) {
            if ((var0 || !((Databank)var6.get(var7)).isFileBased()) && !((Databank)var6.get(var7)).getName().equals("Last generation")) {
               var3 += ((Databank)var6.get(var7)).size();
            }
         }
      }

      StrategiesSaver.StrategySavedListener var11 = new StrategiesSaver.StrategySavedListener(var3, true);
      var11.onStart();

      for (int var12 = 0; var12 < var2.size() && !stopped; var12++) {
         SQProject var13 = (SQProject)var2.get(var12);
         ArrayList var14 = var13.getDatabanks().list();

         for (int var8 = 0; var8 < var14.size() && !stopped; var8++) {
            Databank var9 = (Databank)var14.get(var8);
            if ((var0 || !var9.isFileBased()) && !((Databank)var14.get(var8)).getName().equals("Last generation")) {
               syncDatabankToFiles(var13, var9, var11, var1);
            }
         }
      }

      if (stopped) {
         Log.info("Saving stopped by user");
         var11.onFinish(L.t("Saving stopped by user", new Object[0]), var3);
      } else {
         var11.onFinish(null, var3);
      }

      try {
         Thread.sleep(1000L);
      } catch (Exception var10) {
      }
   }

   private static void syncDatabankToFiles(SQProject var0, Databank var1, StrategiesSaver.StrategySavedListener var2, String var3) {
      if (MainApp.AppLoaded) {
         if (!var1.isLoading()) {
            if (!MainApp.runInConsoleWithoutActiveWebserver() || var1.isLoaded()) {
               int var4 = 0;
               int var5 = 0;
               int var6 = 0;
               long var7 = System.currentTimeMillis();

               try {
                  StrategySaver var9 = new StrategySaver();
                  String var10 = null;
                  ArrayList var11 = var1.getRecordKeys();
                  File var12 = new File(var1.getDatabankFolder());
                  if (!var12.exists()) {
                     var12.mkdirs();
                  }

                  File[] var13 = var12.listFiles();
                  if (var13 == null) {
                     return;
                  }

                  var6 = var13.length;
                  Int2ObjectOpenHashMap var14 = new Int2ObjectOpenHashMap();

                  for (int var15 = 0; var15 < var13.length && !stopped; var15++) {
                     File var16 = var13[var15];
                     String var17 = var16.getAbsolutePath();
                     int var18 = SQUtils.betterHashCode(var17);
                     String[] var19 = (String[])var14.get(var18);
                     if (var19 == null) {
                        var14.put(var18, new String[]{var17});
                     } else {
                        int var20 = var19.length;
                        var19 = Arrays.copyOf(var19, var20 + 1);
                        var19[var20] = var17;
                        var14.put(var18, var19);
                     }
                  }

                  for (int var33 = 0; var33 < var11.size() && !stopped; var33++) {
                     try {
                        ResultsGroup var36 = var1.getLocked((String)var11.get(var33), "StrategiesSaver");

                        try {
                           String var38 = new File(var36.getFilePath()).getAbsolutePath();
                           int var40 = SQUtils.betterHashCode(var38);
                           String[] var43 = (String[])var14.get(var40);
                           if (var43 != null) {
                              if (var43.length == 1) {
                                 var14.remove(var40);
                              } else {
                                 boolean var44 = true;

                                 for (int var21 = 0; var21 < var43.length; var21++) {
                                    if (var43[var21] != null) {
                                       if (var38.equals(var43[var21])) {
                                          var43[var21] = null;
                                       } else {
                                          var44 = false;
                                       }
                                    }
                                 }

                                 if (var44) {
                                    var14.remove(var40);
                                 }
                              }
                           }

                           if (var36.updated) {
                              try {
                                 var9.save(var36, "sqx", var38, false, 0);
                                 var36.updated = false;
                                 var4++;
                              } catch (Exception var27) {
                                 Log.error("Saving to '" + var38 + "' failed", var27);
                                 String var45 = L.t("Saving strategy '%s' failed. ", new Object[0]);
                                 var10 = String.format(var45, var36.getName()) + var27.getMessage();
                              }
                           }
                        } finally {
                           if (var36 != null) {
                              var36.releaseLock("StrategiesSaver");
                           }
                        }
                     } catch (Exception var29) {
                        if (!(var29 instanceof RecordNotFoundException)) {
                           Log.error("Cannot process strategy '" + (String)var11.get(var33) + "'", var29);
                        }
                     }

                     if (var2 != null) {
                        var2.onSave(var10);
                        var10 = null;
                     }
                  }

                  if (var14 != null) {
                     IntIterator var34 = var14.keySet().iterator();

                     while (var34.hasNext()) {
                        int var37 = (Integer)var34.next();
                        String[] var39 = (String[])var14.get(var37);
                        if (var39 != null) {
                           for (int var41 = 0; var41 < var39.length; var41++) {
                              if (var39[var41] != null) {
                                 new File(var39[var41]).delete();
                                 var5++;
                              }
                           }
                        }
                     }
                  }

                  if (var4 > 0 || var5 > 0) {
                     var13 = var12.listFiles();
                     if (var13 == null) {
                        return;
                     }

                     int var35 = var13 == null ? 0 : var13.length;
                     Log.info(
                        L.t(
                           "Databank '%s/%s' saved - files before sync %d / after sync %d / saved %d / removed %d in %.2f s. Sync triggered by '%s'.",
                           new Object[]{var0.getName(), var1.getName(), var6, var35, var4, var5, (System.currentTimeMillis() - var7) / 1000.0, var3}
                        )
                     );
                     var0.printToLog(
                        L.t(
                           "Databank '%s/%s' saved - files before sync %d / after sync %d / saved %d / removed %d in %.2f s.",
                           new Object[]{var0.getName(), var1.getName(), var6, var35, var4, var5, (System.currentTimeMillis() - var7) / 1000.0}
                        )
                     );
                  }
               } catch (Exception var30) {
                  Log.error("Error while syncing databank " + var1.getName() + " into files", var30);
                  if (var2 != null) {
                     var2.onSave(L.t("Syncing databank %s failed - %s", new Object[]{var1.getName(), var30.getMessage()}));
                  }
               }
            }
         }
      }
   }

   public static void syncDatabank(Databank var0, boolean var1, String var2) {
      SQProject var3 = null;

      try {
         if (var0.getName().equals("Last generation")) {
            return;
         }

         int var4 = var0.size();
         stopped = false;
         StrategiesSaver.StrategySavedListener var5 = var1 ? new StrategiesSaver.StrategySavedListener(var4, false) : null;
         if (var5 != null) {
            var5.onStart();
         }

         var3 = ProjectEngine.get(var0.getProject());
         syncDatabankToFiles(var3, var0, var5, var2);
         if (stopped) {
            Log.info("Saving stopped by user");
            if (var5 != null) {
               var5.onFinish(L.t("Saving stopped by user", new Object[]{false}), var4);
            }
         } else if (var5 != null) {
            var5.onFinish(null, var4);
         }
      } catch (Exception var6) {
         Log.error("Synchronizing strategies failed", var6);
         if (var3 != null) {
            var3.printToLog(L.t("Synchronizing strategies failed - ", new Object[0]) + var6.getMessage());
         }
      }
   }

   public static void stop() {
      stopped = true;
   }

   private static class StrategySavedListener {
      private double strategiesProcessed = 0.0;
      private int totalCount;
      private DataToSend data;
      private JSONObject info;

      public StrategySavedListener(int var1, boolean var2) {
         this.totalCount = var1;
         this.data = new DataToSend("saveAllProgress");
         this.info = new JSONObject();
         this.data.setDataObject(this.info);
         this.info.put("isAppExit", var2);
         this.info.put("totalCount", var1);
      }

      public void onStart() {
         this.info.put("progress", 0);
         this.info.put("currentCount", 0);
         SQWebSocketManager.addToDataQueue(this.data, "SQUANT");
      }

      public void onSave(String var1) {
         this.info.put("progress", (int)(this.strategiesProcessed++ / this.totalCount * 100.0));
         this.info.put("currentCount", this.strategiesProcessed);
         this.info.put("totalCount", this.totalCount);
         if (var1 != null) {
            this.info.put("error", var1);
         } else {
            this.info.remove("error");
         }

         SQWebSocketManager.addToDataQueue(this.data, "SQUANT");
      }

      public void onFinish(String var1, int var2) {
         this.info.put("progress", 100);
         if (var1 != null) {
            this.info.put("error", var1);
         } else {
            this.info.remove("error");
         }

         this.info.put("currentCount", var2);
         SQWebSocketManager.addToDataQueue(this.data, "SQUANT");
      }
   }
}
