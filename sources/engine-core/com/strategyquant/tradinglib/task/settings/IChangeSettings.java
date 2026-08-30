package com.strategyquant.tradinglib.task.settings;

import com.strategyquant.datalib.data.DataException;
import com.strategyquant.tradinglib.project.SQProject;
import java.text.ParseException;
import java.util.HashMap;

public interface IChangeSettings {
   String applyChanges(HashMap<String, Object> var1, SQProject var2) throws DataException, ParseException, Exception;
}
