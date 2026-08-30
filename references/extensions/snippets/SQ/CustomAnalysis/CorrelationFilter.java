package SQ.CustomAnalysis;

import com.strategyquant.lib.*;
import com.strategyquant.tradinglib.correlation.*;
import com.strategyquant.tradinglib.project.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import java.util.ArrayList;

import com.strategyquant.tradinglib.project.ProjectEngine;

public class CorrelationFilter extends CustomAnalysisMethod {
    
    public CorrelationFilter() {
        super("CorrelationFilter", TYPE_PROCESS_DATABANK);
    }
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        return true;
    }
    

    @Override
    public ArrayList<ResultsGroup> processDatabank(String projectName, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        int period = CorrelationPeriodTypes.DAY;
        double maxCorrelation = Double.parseDouble(this.getInputArgs());

        SQProject project = ProjectEngine.get(projectName);
        Databank databank = project.getDatabanks().get(databankName);
        
        ArrayList<ResultsGroup> rgSource = new ArrayList<>(databank.getRecords());
        
        ArrayList<ResultsGroup> rgTarget = new ArrayList<ResultsGroup>();
		for (int i = 0; i < rgSource.size(); i++) {
			rgTarget.add(rgSource.get(i).clone());
		}

		// Order results by fitness - descending
        rgTarget.sort((o1, o2) -> -Double.valueOf(o1.getFitness()).compareTo(Double.valueOf(o2.getFitness())));

        for (int i = 0; i < rgTarget.size(); i++) {
            ResultsGroup focusStrat = rgTarget.get(i).clone();
            String focusStratName = focusStrat.getName();

            for (int j = i+1; j < rgTarget.size(); j++) {
                ResultsGroup subStrat = rgTarget.get(j).clone();
                String subStratName = subStrat.getName();

                CorrelationType corrType = CorrelationTypes.getInstance().findClassByName("ProfitLoss");
                CorrelationComputer correlationComputer = new CorrelationComputer();
                CorrelationPeriods correlationPeriods = precomputePeriodsAP(focusStrat, subStrat, period, corrType);
                double corrValue = SQUtils.round2(correlationComputer.computeCorrelation(false, focusStratName, subStratName, focusStrat.orders(), subStrat.orders(), correlationPeriods, corrType));

                if (corrValue > maxCorrelation) {
                    rgTarget.remove(j);
                    j--;
                }
            }
        }
		return rgTarget;
    }
        
    private CorrelationPeriods precomputePeriodsAP(ResultsGroup paramResultsGroup1, ResultsGroup paramResultsGroup2, int paramInt, CorrelationType paramCorrelationType) throws Exception {
        CorrelationPeriods correlationPeriods = new CorrelationPeriods();
        TimePeriod timePeriod = CorrelationLib.getPeriod(paramResultsGroup1.orders());
        long l1 = timePeriod.from;
        long l2 = timePeriod.to;
        timePeriod = CorrelationLib.getPeriod(paramResultsGroup2.orders());
        if (timePeriod.from < l1) {
            l1 = timePeriod.from;
        }
        if (timePeriod.to > l2) {
            l2 = timePeriod.to;
        }

        TimePeriods timePeriods1 = CorrelationLib.generatePeriods(paramInt, l1, l2);
        TimePeriods timePeriods2 = timePeriods1.clone();

        paramCorrelationType.computePeriods(paramResultsGroup1.orders(), paramInt, timePeriods1);
        correlationPeriods.put(paramResultsGroup1.getName(), timePeriods1);

        paramCorrelationType.computePeriods(paramResultsGroup2.orders(), paramInt, timePeriods2);
        correlationPeriods.put(paramResultsGroup2.getName(), timePeriods2);

        return correlationPeriods;
    }     
}
