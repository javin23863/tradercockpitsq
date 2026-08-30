package com.strategyquant.tradinglib;

public class CustomCellFormat {
   public static final String ValuePositive = "value-positive";
   public static final String ValueNegative = "value-negative";
   public static final String BackgroundOOS = "background-oos";
   public String textStyle;
   public String backgroundStyle;
   public String customStyle;

   public String getHtmlContent(Object var1) {
      String var2 = var1 + "";
      if (this.customStyle == null || this.textStyle == null && this.backgroundStyle == null) {
         if (this.customStyle != null) {
            var2 = "<div style='" + this.customStyle + "'>" + var1 + "</div>";
         } else if (this.textStyle != null || this.backgroundStyle != null) {
            var2 = this.textStyle != null ? this.textStyle + "," : "";
            var2 = var2 + (this.backgroundStyle != null ? this.backgroundStyle + "," : "");
            var2 = "<div class='" + (var2.isEmpty() ? var2 : var2.substring(0, var2.length() - 1)) + "'>" + var1 + "</div>";
         }
      } else {
         var2 = this.textStyle != null ? this.textStyle + "," : "";
         var2 = var2 + (this.backgroundStyle != null ? this.backgroundStyle + "," : "");
         var2 = "<div class='" + (var2.isEmpty() ? var2 : var2.substring(0, var2.length() - 1)) + "' style='" + this.customStyle + "'>" + var1 + "</div>";
      }

      return var2;
   }

   public void reset() {
      this.textStyle = null;
      this.backgroundStyle = null;
      this.customStyle = null;
   }
}
