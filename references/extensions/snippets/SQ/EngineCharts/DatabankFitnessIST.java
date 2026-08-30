package SQ.EngineCharts;

import org.json.JSONObject;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;

public class DatabankFitnessIST extends EngineChart {

	private TimeSeriesLineChart chart = null;
	
    private TimeSeries bestSeries = null;
    private TimeSeries topAvgSeries = null;
    private TimeSeries avgSeries = null;
        
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
    
	public DatabankFitnessIST() {
		super(L.tsq("Databank Fitness - IS Training"), TYPE_PROJECT_BASED);
		this.chart = new TimeSeriesLineChart();
		this.chart.yAxisRangeMin = 0;
		this.chart.yAxisRangeMax = 1;
		this.chart.maxXTicksLimit = 4;
		this.chart.showLegend = true;
				
		this.bestSeries = new TimeSeries(L.t("Top Strategy"), TimeSeriesLineChart.PointsCount);
		this.bestSeries.color = ChartsConst.COLOR_GREEN;
		this.chart.addSeries(bestSeries);
		
		this.topAvgSeries = new TimeSeries(L.t("Top 10 Avg"), TimeSeriesLineChart.PointsCount);
		this.topAvgSeries.color = ChartsConst.COLOR_BLUE;
		this.chart.addSeries(topAvgSeries);
		
		this.avgSeries = new TimeSeries(L.t("All Avg"), TimeSeriesLineChart.PointsCount);
		this.avgSeries.color = ChartsConst.COLOR_RED;
		this.chart.addSeries(avgSeries);
	}

	//------------------------------------------------------------------------
	
	@Override
	public JSONObject print() {	
		JSONObject data = new JSONObject();
		
		data.put("type", DATA_TYPE_CHART);
		data.put("chart", this.chart.toJSON());
	
		return data;
	}
		
	//------------------------------------------------------------------------
	
	@Override
	public void addNextValue() {
        try {  
        	Databank databank = project.getResultsDatabank();
        	if(databank == null) {
        		return;
        	}

        	if(!project.isRunning()) {
        		//reset();
        		return;
        	}
        	
       		addValueFromDatabank(databank);
        } catch(Exception e) {
        	Log.error("Exc.", e);
        }
    }

	//------------------------------------------------------------------------

	private void addValueFromDatabank(Databank databank) {
		FitnessCollectionData data = databank.getFitnessData();
		
		long time = System.currentTimeMillis();
		
    	bestSeries.addValue(time, data.bestFitnessIST);
    	topAvgSeries.addValue(time, data.topAvgFitnessIST);
    	avgSeries.addValue(time, data.avgFitnessIST);
	}

	//------------------------------------------------------------------------
	
	private void reset() {
    	bestSeries.clear();
    	topAvgSeries.clear();
    	avgSeries.clear();
	}

	//------------------------------------------------------------------------

}