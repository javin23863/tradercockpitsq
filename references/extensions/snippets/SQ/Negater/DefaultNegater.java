package SQ.Negater;

import com.strategyquant.lib.*;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(1000000)
public class DefaultNegater extends Negater {
	public static final Logger Log = LoggerFactory.getLogger("DefaultNegater");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		// copy parameters from one block to another
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);

		ParametersHelper.negateDataSeries(block, oppositeBlock);

		return oppositeBlock;		
	}
}
