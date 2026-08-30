package com.strategyquant.datalib.data;

import com.strategyquant.datalib.DataInfo;
import java.util.HashMap;

public class DataInfoCache extends HashMap<String, DataInfo> {
   public long lastTimeUpdated = -1L;
}
