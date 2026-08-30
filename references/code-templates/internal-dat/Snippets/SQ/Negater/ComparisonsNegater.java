package SQ.Negater;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(10000)
public class ComparisonsNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Comparison && blockType != BlockSuperTypes.Property) {
			return null;
		}
		
		// it is comparison
		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		// copy parameters from one block to another
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);

		ParametersHelper.negateDataSeries(block, oppositeBlock);

		return oppositeBlock;
	}
		
}
