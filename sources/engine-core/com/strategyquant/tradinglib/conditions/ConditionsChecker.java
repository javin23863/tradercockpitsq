package com.strategyquant.tradinglib.conditions;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionsChecker {
   public static final Logger Log = LoggerFactory.getLogger("ConditionsChecker");
   public String dismissalMessage;
   public HashMap<String, Object> leftValues = new HashMap<>();
   public HashMap<String, Object> rightValues = new HashMap<>();

   public boolean check(ResultsGroup var1, ArrayList<Condition> var2) {
      for (int var3 = 0; var3 < var2.size(); var3++) {
         Condition var4 = (Condition)var2.get(var3);

         try {
            if (!this.check(var1, var4)) {
               return false;
            }
         } catch (CrossCheckDataNotExistException var6) {
         } catch (Exception var7) {
            Log.error("Error while checking condition", var7);
            this.dismissalMessage = String.format("Error while evaluating conditions: %s", var7.getMessage());
            return false;
         }
      }

      return true;
   }

   public boolean check(ResultsGroup var1, Condition var2) throws Exception {
      boolean var3 = false;
      this.leftValues.clear();
      this.rightValues.clear();
      byte[] var4 = this.getSampleType(var2.getLeftSideConditionValue());
      byte[] var5 = this.getSampleType(var2.getRightSideConditionValue());
      int[] var6 = this.getAdditionalMarket(var2.getLeftSideConditionValue(), var1);
      int[] var7 = this.getAdditionalMarket(var2.getRightSideConditionValue(), var1);

      for (int var8 = 0; var8 < var4.length; var8++) {
         for (int var9 = 0; var9 < var5.length; var9++) {
            for (int var10 = 0; var10 < var6.length; var10++) {
               for (int var11 = 0; var11 < var7.length; var11++) {
                  this.leftValues.put("SampleType", var4[var8]);
                  this.leftValues.put("AdditionalMarket", var6[var10]);
                  this.rightValues.put("SampleType", var5[var9]);
                  this.rightValues.put("AdditionalMarket", var7[var11]);
                  var3 = var2.isMet(var1, this.leftValues, this.rightValues);
                  if (!var3) {
                     this.dismissalMessage = var2.getAsText(var1, this.leftValues, this.rightValues);
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   public int[] getAdditionalMarket(IConditionValue var1, ResultsGroup var2) throws Exception {
      String var4 = var1.getCrossCheckSettingName();
      int[] var3;
      if (var4 != null && var4.equals("RetestOnAdditionalMarkets") && var2.additionalMarketKeys().size() > 0) {
         byte var5 = Byte.valueOf(var1.getConditionParamValue("AdditionalMarket", "0"));
         if (var5 > 0) {
            var3 = new int[]{var5};
         } else {
            List var6 = var2.additionalMarketKeys();
            var3 = new int[var6.size()];

            for (int var7 = 0; var7 < var6.size(); var7++) {
               var3[var7] = var7 + 1;
            }
         }
      } else {
         var3 = new int[]{0};
      }

      return var3;
   }

   public byte[] getSampleType(IConditionValue var1) {
      byte var3 = Byte.valueOf(var1.getConditionParamValue("SampleType", "127"));
      byte[] var2;
      if (var3 == 66) {
         var2 = new byte[10];

         for (int var4 = 0; var4 < 10; var4++) {
            var2[var4] = OutOfSample.getISVPart(var4 + 1);
         }
      } else if (var3 == 77) {
         var2 = new byte[10];

         for (int var5 = 0; var5 < 10; var5++) {
            var2[var5] = OutOfSample.getOOSPart(var5 + 1);
         }
      } else {
         var2 = new byte[]{var3};
      }

      return var2;
   }
}
