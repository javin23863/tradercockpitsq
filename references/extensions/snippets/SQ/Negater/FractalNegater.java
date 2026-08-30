package SQ.Negater;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

import SQ.Blocks.Indicators.Fibo.Fibo;
import SQ.Blocks.Indicators.Fractal.Fractal;

@SortOrder(900000)
public class FractalNegater extends Negater {
	public static final Logger Log = LoggerFactory.getLogger("FiboNegater");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(!(block instanceof Fractal)) {
			// it is not Fractal block, skip
			return null;
		}

		IBlock oppositeBlock = Blocks.getOppositeBlock(block);

		// standard negation first
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);
		ParametersHelper.negateDataSeries(block, oppositeBlock);

		// now special Fractal negation
		Fractal opposite = (Fractal) oppositeBlock;
		
		Field lineField = getOutputIndexField(block);
		Field oppositeLineField = getOutputIndexField(oppositeBlock);
				
		if(lineField == null || oppositeLineField == null) {
			return null;
		}
				
		try {
			int pivotLine = lineField.getInt(block);
			int oppositePivotLine = (pivotLine == 1 ? 0 : 1);

			oppositeLineField.set(oppositeBlock, oppositePivotLine);		
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new BlockDefinitionException("Exception setting value of parameter OutputIndex found!", e);
		}
		
		return opposite;		
	}
}
