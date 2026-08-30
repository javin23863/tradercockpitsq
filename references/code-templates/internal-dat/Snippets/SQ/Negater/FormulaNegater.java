package SQ.Negater;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(8000)
public class FormulaNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Formula) {
			return null;
		}
		
		// it should be function
		IBlock oppositeBlock = block.clone(false, strategy);
		
		// negate parameters in the cloned block
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);

/*		
		for(String paramName : ParametersHelper.getParameters(block)) {
			Object paramValue = ParametersHelper.getParameterValue(block, paramName);
			
			if(paramValue instanceof IBlock) {
				paramValue = negatersList.negate((IBlock) paramValue, strategy);
			}
			
			ParametersHelper.setParameterValue(oppositeBlock, paramName, paramValue);
		}
*/		
		return oppositeBlock;
	}
		
}
