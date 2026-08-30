package SQ.Negater;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.BlockSuperTypes;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

import SQ.Internal.CDataIndy;

@SortOrder(1000)
public class CDataIndyNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(!(block instanceof CDataIndy)) {
			return null;
		}
		
		CDataIndy oppositeBlock = ((CDataIndy) block).cloneCDataIndy();

		oppositeBlock.negateValueIndex();
		
		return oppositeBlock;
	}
		
}
