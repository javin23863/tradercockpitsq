package com.strategyquant.datalib.session;

public class SessionNoSession extends Session {
   SessionNoSession() {
      super("No Session", null);
   }

   public void checkTimeIsInSession(long var1, SessionStatus var3) {
      var3.isInSession = true;
   }
}
