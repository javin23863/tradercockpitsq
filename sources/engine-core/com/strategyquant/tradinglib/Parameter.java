package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
   int defaultSeries() default 0;

   int defaultChartIndex() default 0;

   String defaultValue() default "Null";

   String name() default "Null";

   String category() default "Default";

   double minValue() default 1.0;

   double maxValue() default 10000.0;

   double step() default 1.0;

   boolean allowAny() default false;

   boolean showIfDefault() default true;

   String postfix() default "Null";

   boolean isPeriod() default false;

   double builderMinValue() default -9999999.0;

   double builderMaxValue() default -9999999.0;

   double builderStep() default -9999999.0;

   int decimals() default 2;

   String standsFor() default "Null";
}
