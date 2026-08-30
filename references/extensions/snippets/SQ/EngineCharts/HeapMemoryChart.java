package SQ.EngineCharts;

import java.lang.management.MemoryUsage;

import org.json.JSONObject;

import com.strategyquant.lib.*;
import com.strategyquant.tradinglib.*;

public class HeapMemoryChart extends EngineChart {
	private TimeSeriesAreaChart chart = null;
	
    private TimeSeries memoryUsageSeries = null;
    private TimeSeries heapSizeSeries = null;
	private long checks = 0;

	public HeapMemoryChart() {
		super(L.t("Heap memory chart"), TYPE_GLOBAL);
				
		this.chart = new TimeSeriesAreaChart();
		this.chart.yAxisTitle = L.t("Memory");
		this.chart.xAxisTitle = L.t("Time");
		this.chart.yAxisRangeMin = 0;
		this.chart.yAxisRangeMax = new Float(this.maxHeapMemory * 1.1).longValue();
		this.chart.maxXTicksLimit = 4;
		this.chart.maxYTicksLimit = 6;
		this.chart.yAxisTicksMemory = true;
				
		this.memoryUsageSeries = new TimeSeries(L.t("Memory Usage"), TimeSeriesLineChart.PointsCount);
		this.memoryUsageSeries.color = "#FF6E66"; 
		
		this.heapSizeSeries = new TimeSeries(L.t("Heap Size"), TimeSeriesLineChart.PointsCount);
		this.heapSizeSeries.color = "#BFDFBF";
		
		this.chart.addSeries(this.memoryUsageSeries);
		this.chart.addSeries(this.heapSizeSeries);
	}

	@Override
	public JSONObject print() {
		JSONObject data = new JSONObject();
		
		data.put("type", DATA_TYPE_CHART);
		data.put("chart", this.chart.toJSON());
			
		checks ++;
		if(checks % 5000 == 1) {
			// nezakomentoval jsem, ale zvysil jse ten delitel ze 100 na 5000. 
			// nevolat zbytecne System.gc(), pri hodne strategiich to udela peak na CPu
			System.gc();
		}
		
		return data;
	}

	@Override
	public void addNextValue() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMegabytes = heapUsage.getUsed() / MEGABYTE;
        long heapSizeMegabytes = heapUsage.getCommitted() / MEGABYTE;

        try {
        	memoryUsageSeries.addValue(System.currentTimeMillis(), usedMegabytes);
            heapSizeSeries.addValue(System.currentTimeMillis(), heapSizeMegabytes);
        } catch(Exception e) {
        	Log.error("Exc.", e);
        }
    }
}
