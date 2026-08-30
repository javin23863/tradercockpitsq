package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.lib.MemoryInfo;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.memory.OffHeapMemory;
import com.strategyquant.tradinglib.EngineChart;
import org.json.JSONArray;
import org.json.JSONObject;

public class OffHeapMemoryInfoChart extends EngineChart {
   MemoryInfo offHeapInfo = new MemoryInfo();

   public OffHeapMemoryInfoChart() {
      super(L.tsq("Off-heap memory info"), 10);
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      OffHeapMemory.getInfo(this.offHeapInfo);
      JSONArray var2 = new JSONArray();
      var2.put(new JSONObject().put("name", L.tsq("Allocated objects")).put("value", this.offHeapInfo.allocatedObjects));
      var2.put(new JSONObject().put("name", L.tsq("Allocated memory")).put("value", SQUtils.formatBytesToHumanFormat(this.offHeapInfo.allocatedMemory)));
      var1.put("items", var2);
      var1.put("type", "grid");
      return var1;
   }

   public void addNextValue() {
   }
}
