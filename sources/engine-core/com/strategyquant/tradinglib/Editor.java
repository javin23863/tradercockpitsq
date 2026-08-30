package com.strategyquant.tradinglib;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Editor {
   int type() default 0;

   String formulaName() default "Null";

   boolean allowVariables() default true;

   String values() default "Null";
}
