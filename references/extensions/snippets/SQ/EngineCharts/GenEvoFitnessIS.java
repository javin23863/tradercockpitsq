package SQ.EngineCharts;

import org.json.JSONObject;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ChartsConst;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.GeneticInfo;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesLineChart;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;

public class GenEvoFitnessIS extends EngineChart {

	private TimeSeriesLineChart chart = null;
	
    private TimeSeries bestSeries = null;
    private TimeSeries best5AvgSeries = null;
    private TimeSeries avgSeries = null;
        
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
    
	public GenEvoFitnessIS() {
		super(L.tsq("Island #1 - Fitness IS"), TYPE_PROJECT_BASED);
		this.chart = new TimeSeriesLineChart();
		this.chart.yAxisRangeMin = 0;
		this.chart.yAxisRangeMax = 1;
		this.chart.maxXTicksLimit = 4;
		this.chart.showLegend = true;
		
		this.bestSeries = new TimeSeries(L.t("Top Strategy"), TimeSeriesLineChart.PointsCount);
		this.bestSeries.color = ChartsConst.COLOR_GREEN;
		this.chart.addSeries(bestSeries);
		
		this.best5AvgSeries = new TimeSeries(L.t("Top Ten Avg"), TimeSeriesLineChart.PointsCount);
		this.best5AvgSeries.color = ChartsConst.COLOR_BLUE;
		this.chart.addSeries(best5AvgSeries);
		
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
        	
       		addValueFromGenEvo();

        } catch(Exception e) {
        	Log.error("Exc.", e);
        }
    }

	//------------------------------------------------------------------------
	
	private void addValueFromGenEvo() {
		GeneticInfo geneticInfo = project.getGeneticInfo();
    	double bestFitness = 0;
    	double best5Fitness = 0;
    	double avgFitness = 0;

		if(geneticInfo.isGeneticBuild()) {
			GPFitnessEvolutionData[] islandsEvoData = geneticInfo.getIslandsEvoData();
			
			if(islandsEvoData != null && islandsEvoData.length > 0) {
				GPFitnessEvolutionData islandEvoData = islandsEvoData[0];
				
				FitnessCollectionData generationData = islandEvoData.generationData[islandEvoData.generation];
				
				if(generationData != null) {
					bestFitness = generationData.bestFitnessIS;
					best5Fitness = generationData.topAvgFitnessIS;
					avgFitness = generationData.avgFitnessIS;
				}
			}
		}
		
    	bestSeries.addValue(System.currentTimeMillis(), bestFitness);
    	best5AvgSeries.addValue(System.currentTimeMillis(), best5Fitness);
    	avgSeries.addValue(System.currentTimeMillis(), avgFitness);			

	}

	//------------------------------------------------------------------------
	
	private void reset() {
    	bestSeries.clear();
    	best5AvgSeries.clear();
    	avgSeries.clear();
	}

	//------------------------------------------------------------------------

}