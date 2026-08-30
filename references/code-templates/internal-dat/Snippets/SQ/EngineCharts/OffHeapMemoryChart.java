package SQ.EngineCharts;

import org.json.JSONObject;

import com.strategyquant.lib.*;
import com.strategyquant.lib.memory.OffHeapMemory;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class OffHeapMemoryChart extends EngineChart {
	private TimeSeriesAreaChart chart = null;
	private TimeSeries series = null;
	
	MemoryInfo offHeapInfo = new MemoryInfo();
	
	public OffHeapMemoryChart() {
		super(L.tsq("Off-heap memory chart"), TYPE_GLOBAL);
		
		this.chart = new TimeSeriesAreaChart();
		this.chart.yAxisTitle = L.t("Memory");
		this.chart.xAxisTitle = L.t("Time");
		this.chart.maxXTicksLimit = 4;
		this.chart.yAxisRangeMin = 0;
		this.chart.maxYTicksLimit = 6;
		this.chart.yAxisTicksMemory = true;
			
		this.series = new TimeSeries(L.t("Off-Heap Size"), TimeSeriesLineChart.PointsCount);
		this.series.color = "#FF6E66"; 
		this.chart.addSeries(series);
	}
   
	@Override
	public JSONObject print() {	
		JSONObject data = new JSONObject();
		
		data.put("type", DATA_TYPE_CHART);
		data.put("chart", this.chart.toJSON());
			
		return data;
	}
	
	@Override
	public void addNextValue() {
		OffHeapMemory.getInfo(offHeapInfo);
        long usedMegabytes = offHeapInfo.allocatedMemory / MEGABYTE;

        if(usedMegabytes<0) {
        	usedMegabytes = 0;
        }
        
        try {
        	series.addValue(System.currentTimeMillis(), usedMegabytes);
        } catch(Exception e) {
        	Log.error("Exc.", e);
        }
    }
}
