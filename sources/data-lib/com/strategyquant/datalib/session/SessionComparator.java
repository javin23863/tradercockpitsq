package com.strategyquant.datalib.session;

import java.util.Comparator;

public class SessionComparator implements Comparator<Session> {
   public int compare(Session var1, Session var2) {
      if ("No Session".equals(var1.getSessionName()) && var1.getSessionName().equals(var2.getSessionName())) {
         return 0;
      } else {
         return "No Session".equals(var1.getSessionName())
            ? -1
            : ("No Session".equals(var2.getSessionName()) ? 1 : var1.getSessionName().compareTo(var2.getSessionName()));
      }
   }
}
