package com.strategyquant.tradinglib;

public class Editors {
   public static final int RecognizeByType = 0;
   public static final int Spinner = 10;
   public static final int SpinnerWithVariables = 20;
   public static final int Editbox = 30;
   public static final int Selection = 40;
   public static final int SelectionWithVariables = 50;
   public static final int SelectionSymbols = 60;
   public static final int SelectionSymbolsWithAny = 70;
   public static final int SelectionVariables = 80;
   public static final int SelectionVariablesWithAny = 90;
   public static final int BooleanVariables = 100;
   public static final int Time = 110;
   public static final String WizardSpinner = "jspinner";
   public static final String WizardSpinnerVar = "jspinnerVar";
   public static final String WizardEditbox = "editbox";
   public static final String WizardCombo = "combo";
   public static final String WizardSymbolVar = "symbolVar";
   public static final String WizardSymbolVarWithAny = "symbolVarWithAny";
   public static final String WizardComboVar = "comboVar";
   public static final String WizardComboVarWithAny = "comboVarWithAny";
   public static final String WizardBooleanVar = "booleanVar";
   public static final int Formula = 200;

   public static String convertToWizardConstant(int var0) throws BlockDefinitionException {
      switch (var0) {
         case 10:
            return "jspinner";
         case 20:
            return "jspinnerVar";
         case 30:
            return "editbox";
         case 40:
            return "combo";
         case 50:
            return "comboVar";
         case 60:
            return "symbolVar";
         case 70:
            return "symbolVarWithAny";
         case 80:
            return "comboVar";
         case 90:
            return "comboVarWithAny";
         case 100:
            return "booleanVar";
         case 110:
            return "Time";
         case 200:
            return "Formula";
         default:
            throw new BlockDefinitionException("Unknowd parameter editor type: " + var0);
      }
   }

   public static String convertTimeToHHMM(String var0) {
      if (var0.equals("NaN")) {
         return "0000";
      }

      int var1 = Integer.parseInt(var0);
      int var2 = var1 / 3600;
      int var3 = (var1 - var2 * 3600) / 60;
      return String.format("%02d%02d", var2, var3);
   }
}
