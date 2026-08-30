package com.strategyquant.pluginlib.program;

public class ProgramMethodDoesntExistException extends Exception {
   public ProgramMethodDoesntExistException(String var1) {
      super("Program method '" + var1 + "' doesn't exist");
   }
}
