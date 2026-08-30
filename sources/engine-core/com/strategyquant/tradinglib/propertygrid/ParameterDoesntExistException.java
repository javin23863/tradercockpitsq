package com.strategyquant.tradinglib.propertygrid;

public class ParameterDoesntExistException extends Exception {
   public ParameterDoesntExistException(String var1) {
      super("Parameter '" + var1 + "' doesn't exist");
   }
}
