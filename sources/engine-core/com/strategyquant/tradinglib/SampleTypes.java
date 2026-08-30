package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class SampleTypes {
   public static final byte FullSample = 127;
   public static final byte InSample = 10;
   public static final byte InSampleTraining = 11;
   public static final byte InSampleValidation = 40;
   public static final byte InSampleValidation1 = 41;
   public static final byte InSampleValidation2 = 42;
   public static final byte InSampleValidation3 = 43;
   public static final byte InSampleValidation4 = 44;
   public static final byte InSampleValidation5 = 45;
   public static final byte InSampleValidation6 = 46;
   public static final byte InSampleValidation7 = 47;
   public static final byte InSampleValidation8 = 48;
   public static final byte InSampleValidation9 = 49;
   public static final byte InSampleValidation10 = 50;
   public static final byte OutOfSample = 20;
   public static final byte OutOfSample1 = 21;
   public static final byte OutOfSample2 = 22;
   public static final byte OutOfSample3 = 23;
   public static final byte OutOfSample4 = 24;
   public static final byte OutOfSample5 = 25;
   public static final byte OutOfSample6 = 26;
   public static final byte OutOfSample7 = 27;
   public static final byte OutOfSample8 = 28;
   public static final byte OutOfSample9 = 29;
   public static final byte OutOfSample10 = 30;
   public static final byte InSampleValidationEvery = 66;
   public static final byte OutOfSampleEvery = 77;
   public static final byte NoTrade = 99;

   public static byte[] types() {
      return new byte[]{127, 10, 20};
   }

   public static String typeToString(byte var0) {
      switch (var0) {
         case 10:
            return L.tsq("IS");
         case 11:
            return L.tsq("IST");
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 65:
         case 67:
         case 68:
         case 69:
         case 70:
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 97:
         case 98:
         default:
            return L.tsq("Full Sample");
         case 20:
            return L.tsq("OOS");
         case 21:
            return L.tsq("OOS1");
         case 22:
            return L.tsq("OOS2");
         case 23:
            return L.tsq("OOS3");
         case 24:
            return L.tsq("OOS4");
         case 25:
            return L.tsq("OOS5");
         case 26:
            return L.tsq("OOS6");
         case 27:
            return L.tsq("OOS7");
         case 28:
            return L.tsq("OOS8");
         case 29:
            return L.tsq("OOS9");
         case 30:
            return L.tsq("OOS10");
         case 40:
            return L.tsq("ISV");
         case 41:
            return L.tsq("ISV1");
         case 42:
            return L.tsq("ISV2");
         case 43:
            return L.tsq("ISV3");
         case 44:
            return L.tsq("ISV4");
         case 45:
            return L.tsq("ISV5");
         case 46:
            return L.tsq("ISV6");
         case 47:
            return L.tsq("ISV7");
         case 48:
            return L.tsq("ISV8");
         case 49:
            return L.tsq("ISV9");
         case 50:
            return L.tsq("ISV10");
         case 66:
            return L.tsq("ISV-Every");
         case 77:
            return L.tsq("OOS-Every");
         case 99:
            return L.tsq("No trade");
      }
   }

   public static boolean isOOS(byte var0) {
      return var0 == 20 || var0 >= 21 && var0 <= 30;
   }

   public static boolean isISV(byte var0) {
      return var0 == 40 || var0 >= 41 && var0 <= 50;
   }

   public static String typeToFullString(byte var0) {
      switch (var0) {
         case 10:
            return L.tsq("In Sample");
         case 11:
            return L.tsq("In sample Training");
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 65:
         case 66:
         case 67:
         case 68:
         case 69:
         case 70:
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 77:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 97:
         case 98:
         default:
            return L.tsq("Full Sample");
         case 20:
            return L.tsq("Out of sample");
         case 21:
            return L.tsq("Out of sample 1");
         case 22:
            return L.tsq("Out of sample 2");
         case 23:
            return L.tsq("Out of sample 3");
         case 24:
            return L.tsq("Out of sample 4");
         case 25:
            return L.tsq("Out of sample 5");
         case 26:
            return L.tsq("Out of sample 6");
         case 27:
            return L.tsq("Out of sample 7");
         case 28:
            return L.tsq("Out of sample 8");
         case 29:
            return L.tsq("Out of sample 9");
         case 30:
            return L.tsq("Out of sample 10");
         case 40:
            return L.tsq("In sample Validation");
         case 41:
            return L.tsq("In sample Validation 1");
         case 42:
            return L.tsq("In sample Validation 2");
         case 43:
            return L.tsq("In sample Validation 3");
         case 44:
            return L.tsq("In sample Validation 4");
         case 45:
            return L.tsq("In sample Validation 5");
         case 46:
            return L.tsq("In sample Validation 6");
         case 47:
            return L.tsq("In sample Validation 7");
         case 48:
            return L.tsq("In sample Validation 8");
         case 49:
            return L.tsq("In sample Validation 9");
         case 50:
            return L.tsq("In sample Validation 10");
         case 99:
            return L.tsq("No trade");
      }
   }

   public static byte[] list() {
      return new byte[]{10, 11, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
   }
}
