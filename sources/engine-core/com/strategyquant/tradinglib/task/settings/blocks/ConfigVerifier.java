package com.strategyquant.tradinglib.task.settings.blocks;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.generator.StrategyTemplateGenerator;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigVerifier {
   private static final Logger Log = LoggerFactory.getLogger(ConfigVerifier.class);

   public static void verifyConfig(Element var0, BlocksConfig var1, TaskSettingsData var2) {
      if (var0 == null) {
         addError(var2, "Invalid XML configuration - missing Settings element.");
      } else {
         checkBlocksConfig(var2, var0, var1);
      }
   }

   private static void checkBlocksConfig(TaskSettingsData var0, Element var1, BlocksConfig var2) {
      Element var3 = var1.getChild("Blocks");
      if (var3 == null) {
         addError(var0, "Cannot find Blocks settings.");
      } else {
         Element var4 = null;
         Element var5 = var1.getChild("Options");
         if (var5 != null) {
            var5 = var5.getChild("BuildTradingOptions");
            if (var5 != null) {
               var4 = var5.getChild("Params");
            }
         }

         Element var6 = var1.getChild("WhatToBuild");
         if (var6 != null) {
            Element var7 = var6.getChild("StrategyType");
            if (var7 != null) {
               String var8 = var7.getAttributeValue("type");
               if (var8 != null && var8.equals("template")) {
                  return;
               }
            }
         }

         boolean var10 = checkOrderTypes(var0, var3);
         boolean var11 = checkExitTypes(var0, var3, var4);
         checkBlocks(var0, var3, var2, var10, var11, var1);
      }
   }

   private static boolean checkExitTypes(TaskSettingsData var0, Element var1, Element var2) {
      boolean var3 = false;
      Element var4 = var1.getChild("ExitTypes");
      if (var4 == null) {
         addError(var0, "Cannot find Exit types settings.");
         return false;
      }

      if (getUsedBlocksCount(var4.getChildren("Block")) == 0) {
         if (!checkExitOnDayFridayIsUsed(var2)) {
            addError(var0, "You have to choose at least one of the Exit Types!");
            return false;
         } else {
            return true;
         }
      } else {
         for (Element var6 : var4.getChildren("Block")) {
            boolean var7 = false;
            int var8 = 0;
            String var9 = var6.getAttributeValue("use");
            if ((var9 == null || !var9.equals("false")) && var6.getAttributeValue("type") != null && var6.getAttributeValue("type").equals("formula")) {
               for (Element var11 : var6.getChildren("Value")) {
                  var7 = true;
                  if (var11.getAttributeValue("use").equals("true")) {
                     var8++;
                     String var12 = var11.getAttributeValue("key");
                     if (var12.contains(".PriceLevel")) {
                        var3 = true;
                     }
                  }
               }

               if (var7 && var8 == 0) {
                  addError(
                     var0, String.format("You have chosen Exit type '%s' to be used, but you didn't select any of its options!", var6.getAttributeValue("key"))
                  );
               }
            }
         }

         return var3;
      }
   }

   private static boolean checkExitOnDayFridayIsUsed(Element var0) {
      if (var0 == null) {
         return false;
      }

      List var1 = var0.getChildren("Param");
      boolean var2 = false;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         Element var4 = (Element)var1.get(var3);
         String var5 = var4.getAttributeValue("key");
         if (var5.equals("ExitAtEndOfDay") || var5.equals("ExitOnFriday")) {
            String var6 = var4.getText();
            if (var6 != null && var6.equals("true")) {
               return true;
            }
         }

         if (var5.equals("LimitTimeRange")) {
            String var7 = var4.getText();
            if (var7 != null && var7.equals("true")) {
               var2 = true;
            }
         }

         if (var5.equals("ExitAtEndOfRange") && var2) {
            String var8 = var4.getText();
            if (var8 != null && var8.equals("true")) {
               return true;
            }
         }
      }

      return false;
   }

   private static int getUsedBlocksCount(List<Element> var0) {
      int var1 = 0;

      for (Element var3 : var0) {
         String var4 = var3.getAttributeValue("use");
         if (var4 != null && var4.equals("true")) {
            var1++;
         }
      }

      return var1;
   }

   private static boolean checkOrderTypes(TaskSettingsData var0, Element var1) {
      Element var2 = var1.getChild("OrderTypes");
      if (var2 == null) {
         addError(var0, "Cannot find Order types settings.");
      }

      boolean var3 = false;
      boolean var4 = false;

      for (Element var6 : var2.getChildren("Block")) {
         if (var6.getAttributeValue("use").equals("true")) {
            var3 = true;

            for (Element var8 : var6.getChild("Generated").getChildren("Param")) {
               if (var8.getAttributeValue("key").equals("#Price#")) {
                  var4 = true;
               }
            }
         }
      }

      if (!var3) {
         addError(var0, "You have to choose at least one of Order Type blocks!");
      }

      return var4;
   }

   private static void checkBlocks(TaskSettingsData var0, Element var1, BlocksConfig var2, boolean var3, boolean var4, Element var5) {
      Element var6 = var1.getChild("BuildingBlocks");
      if (var6 == null) {
         addError(var0, "Cannot find Building blocks settings.");
      } else {
         ConfigVerifier.BlocksExists var7 = new ConfigVerifier.BlocksExists();
         checkStandardBlocks(var7, var6, var0, var2);
         checkCustomDataBlocks(var7, var1);
         checkRandomBlockGroups(var7, var0, var2, var5);
         if (!var7.blocksFound) {
            addError(var0, "You have to choose at least one building block!");
         }

         if (!var7.booleanBlockFound && !var7.valueBlockFound) {
            addError(var0, "You have to choose at least one signal or indicator block!");
         }

         if (var7.valueBlockFound && !var7.comparisonBlocksFound) {
            addError(
               var0,
               "You have chosen some indicators or price values, but no comparison operators!\nEither choose some operators or deselect all indicator / price blocks."
            );
         }

         if (!var7.valueBlockFound && var7.comparisonBlocksFound) {
            addError(
               var0,
               "You have chosen some operators (<, >, Is rising, etc.) blocks, but no price / indicators blocks to compare!\nEither choose some price or indicator blocks, or deselect all operators."
            );
         }

         if (var3) {
            if (!var7.priceLevelFound) {
               addError(var0, "You use Enter at Stop or Limit, you have to choose some Price Level blocks!");
            }

            if (!var7.priceRangeFound) {
            }
         }

         if (var4) {
            if (!var7.priceLevelFound) {
               addError(var0, "You use Indicators in exits (Stop Loss or Profit Target, Trailing Stop etc.), you have to choose some Price Level blocks!");
            }

            if (!var7.priceRangeFound) {
            }
         }

         if ((!var7.priceLevelFound || !var7.priceRangeFound) && usesIndicatorLevelsForExits(var1)) {
            addError(
               var0,
               "You use Indicator levels in some of the exit types (Stop Loss, Profit Target or Trailing stop), but you haven't chosen any Price or Price Range block!"
            );
         }
      }
   }

   private static void checkRandomBlockGroups(ConfigVerifier.BlocksExists var0, TaskSettingsData var1, BlocksConfig var2, Element var3) {
      Element var4 = var3.getChild("WhatToBuild");
      String var5 = var4.getChild("StrategyType").getAttributeValue("type");
      if (var5.equals("template")) {
         Element var6;
         try {
            var6 = StrategyTemplateGenerator.getConfiguredTemplate(var3, null);
            if (var6 == null) {
               return;
            }
         } catch (Exception var14) {
            Log.error("Cannot get template", var14);
            return;
         }

         Element var7 = var6.getChild("Strategy").getChild("RandomGroups");
         if (var7 != null) {
            for (Element var9 : var7.getChildren("Group")) {
               String var10 = var9.getAttributeValue("id");

               for (Element var12 : var9.getChildren("Item")) {
                  String var13 = var12.getAttributeValue("key");
                  if (var13 != null && !var13.equals("")) {
                     checkBlockRandomGroup(var0, var13, var1, var2, var6, var7, var10);
                  } else {
                     addError(var1, "Some block element in Random group doesn't contain 'key' attribute!");
                  }
               }
            }
         }
      }
   }

   private static void checkStandardBlocks(ConfigVerifier.BlocksExists var0, Element var1, TaskSettingsData var2, BlocksConfig var3) {
      for (Element var5 : var1.getChildren("Block")) {
         String var6 = var5.getAttributeValue("use");
         if (!var6.equals("false")) {
            var0.blocksFound = true;
            String var7 = var5.getAttributeValue("key");
            if (var7 == null || var7.equals("")) {
               addError(var2, "Some block element doesn't contain 'key' attribute!");
            } else if (checkBlock(var0, var7, var2, var3)) {
               Element var8 = var5.getChild("Generated");
               if (var8 == null) {
                  addError(var2, String.format("Block '%s' doesn't contain <Generated> child!", var7));
               } else {
                  String var9 = var8.getAttributeValue("weight");
                  if (var9 == null) {
                     addError(var2, String.format("Block '%s' doesn't contain weight attribute in its <Generated> child!", var7));
                  } else {
                     if (!var9.equals("0")) {
                     }

                     if (var5.getChild("Predefined") != null) {
                        for (Element var11 : var5.getChild("Predefined").getChildren()) {
                           String var12 = var11.getAttributeValue("weight");
                           if (var12 != null && !var12.equals("0")) {
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean checkBlock(ConfigVerifier.BlocksExists var0, String var1, TaskSettingsData var2, BlocksConfig var3) {
      if (var1.equals("RandomlyGeneratedPatterns")) {
         return false;
      }

      if (!var1.equalsIgnoreCase("AND") && !var1.equalsIgnoreCase("OR")) {
         String var4 = var1;
         if (var1.contains(".")) {
            var1 = var1.substring(var1.indexOf(".") + 1);
         }

         BlockDefinition var5 = var3.getBlock(var1);
         if (var5 == null) {
            addError(var2, String.format("Cannot find Building block with key '%s'.", var1));
         } else {
            if (var5.superType == 2) {
               var0.comparisonBlocksFound = true;
            }

            if (var5.superType == 1) {
               var0.comparisonBlocksFound = true;
            }

            if (var5.superType == 2 || var5.superType == 3 || var5.superType == 1) {
               var0.booleanBlockFound = true;
            }

            if (var5.superType == 4 && !var4.contains("Stop/Limit Price Range") && !var4.contains("Stop/Limit Price Level")) {
               var0.valueBlockFound = true;
            }

            if (var5.returnType == 7 && var4.contains("Stop/Limit Price Range")) {
               var0.priceRangeFound = true;
            }

            if (var5.returnType == 2 && var4.contains("Stop/Limit Price Level")) {
               var0.priceLevelFound = true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean checkBlockRandomGroup(
      ConfigVerifier.BlocksExists var0, String var1, TaskSettingsData var2, BlocksConfig var3, Element var4, Element var5, String var6
   ) {
      if (var1.equals("RandomlyGeneratedPatterns")) {
         return false;
      }

      if (!var1.equalsIgnoreCase("AND") && !var1.equalsIgnoreCase("OR")) {
         if (var1.contains(".")) {
            var1 = var1.substring(var1.indexOf(".") + 1);
         }

         BlockDefinition var7 = var3.getBlock(var1);
         if (var7 == null) {
            addError(var2, String.format("Cannot find Building block with key '%s'.", var1));
         } else {
            if (var7.superType == 2) {
               var0.comparisonBlocksFound = true;
            }

            if (var7.superType == 2 || var7.superType == 3 || var7.superType == 1) {
               var0.booleanBlockFound = true;
            }

            if (var7.superType == 4) {
               if (randomGroupUsedInPrice(var6, var4)) {
                  var0.priceRangeFound = true;
                  var0.priceLevelFound = true;
               } else {
                  var0.valueBlockFound = true;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean randomGroupUsedInPrice(String var0, Element var1) {
      if (var1.getName().equals("Param")) {
         String var2 = var1.getAttributeValue("key");
         if (var2 != null && var2.equals("#Group#")) {
            String var3 = var1.getText();
            if (var3.equals(var0)) {
               Element var4 = var1.getParentElement();

               for (int var5 = 0; var5 < 6; var5++) {
                  String var6 = var4.getAttributeValue("key");
                  if (var6 != null && var6.equals("#Price#")) {
                     return true;
                  }

                  var4 = var4.getParentElement();
               }
            }
         }
      }

      List var7 = var1.getChildren();
      if (var7 != null && var7.size() > 0) {
         for (int var8 = 0; var8 < var7.size(); var8++) {
            Element var9 = (Element)var7.get(var8);
            if (randomGroupUsedInPrice(var0, var9)) {
               return true;
            }
         }
      }

      return false;
   }

   private static void checkCustomDataBlocks(ConfigVerifier.BlocksExists var0, Element var1) {
      Element var2 = var1.getChild("CustomData");
      if (var2 != null) {
         for (Element var4 : var2.getChildren("Data")) {
            String var5 = var4.getAttributeValue("use");
            if (var5 != null && var5.equals("true")) {
               String var6 = var4.getAttributeValue("name");
               CustomDataInfo var7 = CustomDataManager.getDataInfo(var6);
               if (var7 == null) {
                  Log.info("Cannot find custom data indicator '{}', ignoring!", var6);
               } else {
                  switch (var7.dataType) {
                     case 1:
                        var0.priceLevelFound = true;
                        var0.valueBlockFound = true;
                        break;
                     case 2:
                        var0.valueBlockFound = true;
                        break;
                     case 3:
                        var0.priceRangeFound = true;
                     case 4:
                     case 5:
                     case 6:
                     case 7:
                     case 8:
                     case 9:
                     default:
                        break;
                     case 10:
                     case 11:
                        var0.booleanBlockFound = true;
                  }
               }
            }
         }
      }
   }

   private static boolean usesIndicatorLevelsForExits(Element var0) {
      Element var1 = var0.getChild("ExitTypes");
      if (var1 == null) {
         return false;
      }

      if (getUsedBlocksCount(var1.getChildren("Block")) == 0) {
         return false;
      }

      for (Element var3 : var1.getChildren("Block")) {
         if (var3.getAttributeValue("type") != null && var3.getAttributeValue("type").equals("formula") && var3.getAttributeValue("key").equals("true")) {
            for (Element var5 : var3.getChildren("Value")) {
               if (var5.getAttributeValue("use").equals("true") && var5.getAttributeValue("key").contains("PriceLevel")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static void addError(TaskSettingsData var0, String var1) {
      var0.addError("Blocks", null, var1);
   }

   private static class BlocksExists {
      boolean booleanBlockFound = false;
      boolean priceRangeFound = false;
      boolean priceLevelFound = false;
      boolean blocksFound = false;
      boolean valueBlockFound = false;
      boolean comparisonBlocksFound = false;

      private BlocksExists() {
      }
   }
}
