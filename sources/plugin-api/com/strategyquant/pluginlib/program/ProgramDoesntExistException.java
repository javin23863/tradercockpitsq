package com.strategyquant.pluginlib.program;

public class ProgramDoesntExistException extends Exception {
   public ProgramDoesntExistException(String var1) {
      super("Program '" + var1 + "' doesn't exist");
   }
}
