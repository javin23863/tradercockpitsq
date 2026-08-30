package SQ.Negater;

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

@SortOrder(900000)
public class FiboNegater extends Negater {
	public static final Logger Log = LoggerFactory.getLogger("FiboNegater");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(!block.getClass().getSimpleName().equals("Fibo")) {
			// it is not Fibo block, skip
			return null;
		}

		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		// standard negation first
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);
		ParametersHelper.negateDataSeries(block, oppositeBlock);

		// now special Fibo negation
		Fibo source = (Fibo) block;
		Fibo opposite = (Fibo) oppositeBlock;
		
		opposite.FiboLevel = source.FiboLevel;
		
		return opposite;		
	}
}
