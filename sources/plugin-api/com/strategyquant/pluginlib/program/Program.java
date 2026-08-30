package com.strategyquant.pluginlib.program;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Program {
   public static final Logger Log = LoggerFactory.getLogger(Program.class);
   public static HashMap<String, IProgram> programsMap = new HashMap<>();

   public static IProgram get(String var0) throws ProgramDoesntExistException {
      if (!programsMap.containsKey(var0)) {
         throw new ProgramDoesntExistException(var0);
      } else {
         return programsMap.get(var0);
      }
   }

   public static void register(String var0, IProgram var1) {
      if (programsMap.containsKey(var0)) {
         Log.error("Duplicate registered program with name '" + var0 + "', skipping second registration!");
      } else {
         programsMap.put(var0, var1);
         Log.debug("Program '" + var0 + "' was registered.");
      }
   }

   public static boolean isRegistered(String var0) {
      return programsMap.containsKey(var0);
   }
}
