package SQ.Negater;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(9000)
public class FunctionsNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Value || returnType != ReturnTypes.PriceOrNumber) {
			return null;
		}
		
		// it should be function
		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		// copy parameters from one block to another
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);

		ParametersHelper.negateDataSeries(block, oppositeBlock);
		
		return oppositeBlock;
	}
		
}
