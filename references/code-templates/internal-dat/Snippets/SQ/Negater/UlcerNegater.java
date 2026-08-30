package SQ.Negater;

import SQ.Blocks.Indicators.UlcerIndex.UlcerIndex;
import com.strategyquant.tradinglib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SortOrder(900000)
public class UlcerNegater extends Negater {
	public static final Logger Log = LoggerFactory.getLogger("UlcerNegater");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(!(block instanceof UlcerIndex)) {
			// it is not UlcerIndex block, skip
			return null;
		}

		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		// copy parameters from one block to another
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);

		ParametersHelper.negateDataSeries(block, oppositeBlock);


		UlcerIndex currentUI = (UlcerIndex) block;
		UlcerIndex oppositeUI = (UlcerIndex) oppositeBlock;

		oppositeUI.Mode = (currentUI.Mode == 1 ? 2 : 1);

		return oppositeBlock;		
	}
}
