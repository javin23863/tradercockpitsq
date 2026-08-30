package SQ.EngineCharts;

import org.json.JSONArray;
import org.json.JSONObject;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;

public class GeneticEvolutionInfo extends EngineChart {
	
	public GeneticEvolutionInfo() {
		super(L.tsq("Genetic Evolution info"), TYPE_PROJECT_BASED);
	}

	//------------------------------------------------------------------------
	
	@Override
	public JSONObject print() {
		JSONObject data = new JSONObject();

		GeneticInfo geneticInfo = project.getGeneticInfo();

		JSONArray items = new JSONArray();

		if(!geneticInfo.isGeneticBuild()) {
			items.put(new JSONObject().put("name", L.tsq("No data")).put("value", "No genetic evolution running"));
			
		} else {
			
			GPFitnessEvolutionData[] islands = geneticInfo.getIslandsEvoData();
			
			if(islands != null) 
				for(int i=0; i<Math.min(7, islands.length); i++) {
					if(islands[i] != null) {
						items.put(new JSONObject()
								.put("name", String.format("Island #%d", i+1))
								.put("value", String.format("Generation: %d, Population: %d", islands[i].generation, islands[i].populationSize)));
					}
			}
		}

		data.put("items", items);
		data.put("type", DATA_TYPE_ROWS);
		
		return data;
	}

	//------------------------------------------------------------------------

	@Override
	public void addNextValue() {
	}	
}
