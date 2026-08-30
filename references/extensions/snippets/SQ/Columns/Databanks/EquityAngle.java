package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquityAngle extends DatabankColumn {
    public static final Logger Log = LoggerFactory.getLogger("EquitySlope");

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    public EquityAngle() {
        super(L.tsq("EquityAngle"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, -90, 90);

        setTooltip(L.tsq("Angle of the equity"));

        // Angle is already computed in RSquared, to avoid going through the same algorithm multiple times
        // we have to add dependency on this column here to ensure that this value is already computed
        setDependencies("RSquared", "NetProfit");
    }

    //------------------------------------------------------------------------

    @Override
    public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
        if(ordersList==null || ordersList.isEmpty()) {
            return 0d;
        }

        // compute also angle
        double x_dataLength = stats.getDouble("DataLength");
        double x_netProfit = stats.getDouble("NetProfit");
        double atan2 = Math.atan2(x_netProfit, x_dataLength);

        double angle = Math.toDegrees(atan2);

        return round2(angle);
    }


}