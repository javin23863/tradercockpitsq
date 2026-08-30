package SQ.EngineCharts;

import org.json.JSONObject;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class AcceptedStrategiesPerHour extends EngineChart {
	private TimeSeriesLineChart chart = null;
	private TimeSeries series = null;

	private ProjectRunInfo projectRunInfo = new ProjectRunInfo();
	private ProjectRunInfo oldProjectRunInfo = new ProjectRunInfo();

	private int checksSinceLastChange = 0;
	
	public AcceptedStrategiesPerHour() {
		super(L.tsq("Accepted strategies per hour"), TYPE_PROJECT_BASED);
		
		this.chart = new TimeSeriesLineChart();
		this.chart.yAxisTitle = L.t("Strategies");
		this.chart.xAxisTitle = L.t("Time");
		this.chart.maxXTicksLimit = 4;
		this.chart.yAxisRangeMin = 0;
				
		this.series = new TimeSeries(L.t("Accepted str. per hour"), TimeSeriesLineChart.PointsCount);
		this.series.color = ChartsConst.COLOR_GREEN;
		((TimeSeriesLineChart)this.chart).addSeries(series);
		
    	oldProjectRunInfo.totalJobsDone = -1;
    	checksSinceLastChange  = 0;
	}
 
	@Override
	public JSONObject print() {	
		JSONObject data = new JSONObject();
		
		project.loadTrackingInfo(projectRunInfo, false);
		
		checksSinceLastChange++;
		
		if((oldProjectRunInfo.totalJobsDone == -1 || projectRunInfo.totalJobsDone != oldProjectRunInfo.totalJobsDone) || checksSinceLastChange > 10) {

			// refresh only if something changed
			oldProjectRunInfo.totalJobsDone = projectRunInfo.totalJobsDone;
			checksSinceLastChange = 0;
					
			data.put("type", DATA_TYPE_CHART);
			data.put("chart", this.chart.toJSON());
		
			return data;
		} else {
			return null;
		}
	}
	
	@Override
	public void addNextValue() {
        try {
        	if(!project.isRunning()) {
        		//reset();
        		
        		return;
        	}
        	
        	series.addValue(System.currentTimeMillis(), projectRunInfo.acceptedStrategiesPerHour);
        } catch(Exception e) {
        	Log.error("Exc.", e);
        }
    }
}
