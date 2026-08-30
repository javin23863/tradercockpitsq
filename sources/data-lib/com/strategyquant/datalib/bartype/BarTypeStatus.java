package com.strategyquant.datalib.bartype;

import com.strategyquant.datalib.session.SessionStatus;
import java.io.Serializable;

public class BarTypeStatus implements Serializable {
   public static final int NOT_IN_SESSION = 0;
   public static final int NEW_BAR = 1;
   public static final int EXISTING_BAR = 2;
   public int status;
   public long barTime;
   public SessionStatus sessionStatus = new SessionStatus();
   public boolean convertingToHigherTF = false;
}
