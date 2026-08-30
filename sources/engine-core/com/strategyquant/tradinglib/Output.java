package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Output {
   String name() default "Null";

   String color() default "#FF0000";

   boolean show() default true;

   boolean showZeroValues() default true;

   String chartType() default "line";
}
