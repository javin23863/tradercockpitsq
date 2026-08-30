package SQ.EngineCharts;

import org.json.JSONArray;
import org.json.JSONObject;

import com.strategyquant.lib.*;
import com.strategyquant.lib.memory.OffHeapMemory;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class OffHeapMemoryInfoChart extends EngineChart {
	MemoryInfo offHeapInfo = new MemoryInfo();
	
	public OffHeapMemoryInfoChart() {
		super(L.tsq("Off-heap memory info"), TYPE_GLOBAL);
	}

	@Override
	public JSONObject print() {
		JSONObject data = new JSONObject();

		OffHeapMemory.getInfo(offHeapInfo);
		
		JSONArray items = new JSONArray();
		items.put(new JSONObject().put("name", L.tsq("Allocated objects")).put("value", offHeapInfo.allocatedObjects));
		items.put(new JSONObject().put("name", L.tsq("Allocated memory")).put("value", SQUtils.formatBytesToHumanFormat(offHeapInfo.allocatedMemory)));
/*		
		if(offHeapInfo.totalAllocatedObjects < 999999) {
			items.put(new JSONObject().put("name", "All-time allocated objects").put("value", offHeapInfo.totalAllocatedObjects));
		} else {
			String obj = ((int) offHeapInfo.totalAllocatedObjects/1000) + " k.";
			
			items.put(new JSONObject().put("name", "All-time allocated objects").put("value", obj));
		}
		
		items.put(new JSONObject().put("name", "All-time allocated size").put("value", SQUtils.formatBytesToHumanFormat(offHeapInfo.totalAllocatedMemory)));
		items.put(new JSONObject().put("name", "Reallocations").put("value", offHeapInfo.reallocations));
*/
		data.put("items", items);
		data.put("type", DATA_TYPE_GRID);
		
		return data;
	}

	@Override
	public void addNextValue() {
		// TODO Auto-generated method stub
		
	}	
}
