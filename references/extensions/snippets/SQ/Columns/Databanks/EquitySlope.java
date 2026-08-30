package SQ.Columns.Databanks;

import SQ.Functions.DailyEquityComputer;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import org.jdom2.Element;

public class EquitySlope extends DatabankColumn {
    public static final Logger Log = LoggerFactory.getLogger("EquitySlope");

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public EquitySlope() {
        super(L.tsq("EquitySlope"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, -90, 90);

        setTooltip(L.tsq("EquitySlope - how straight is the equity curve times how big is its angle"));

        // Angle is already computed in RSquared, to avoid going through the same algorithm multiple times
        // we have to add dependency on this column here to ensure that this value is already computed
        setDependencies("RSquared", "EquityAngle");
    }

    //------------------------------------------------------------------------

    @Override
    public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
        if(ordersList==null || ordersList.isEmpty()) {
            return 0d;
        }

        // already computed in RSquared and EquityAngle snippets
        double angle = stats.getDouble("EquityAngle");
        double rsquared = stats.getDouble("RSquared");

        double slope = angle * rsquared;

        return round2(slope);
    }


}