package SQ.Negater;

import SQ.Blocks.Order.Open.EnterAtMarket;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(10000)
public class ActionsNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Action) {
			return null;
		}
		
		IBlock clonedBlock =  block.clone(true, strategy);
		
		ParametersHelper.negateParametersInClonedBlock(block, clonedBlock, negatersList, strategy);

/*		
		for(String paramName : ParametersHelper.getParameters(block)) {
			Object paramValue = ParametersHelper.getParameterValue(block, paramName);
			
			if(paramValue instanceof IBlock) {
				paramValue = negatersList.negate((IBlock) paramValue, strategy);
			}
			
			ParametersHelper.setParameterValue(clonedBlock, paramName, paramValue);
		}
*/		
		ExitMethod[] exitMethods = (ExitMethod[]) ParametersHelper.getFieldValue(block, "ExitMethods");
		if(exitMethods != null) {
			// negate also exit methods
			ExitMethod[] clonedExitMethods = new ExitMethod[exitMethods.length];
			
			for(int i=0; i<exitMethods.length; i++) {
				clonedExitMethods[i] = (ExitMethod) negatersList.negate((IBlock) exitMethods[i], strategy);
			}
			
			ParametersHelper.setFieldValue(clonedBlock, "ExitMethods", clonedExitMethods);
		}
		
		// change direction of the order
		try {
			int currentDir = (int) ParametersHelper.getFieldValue(clonedBlock, "Direction");

			currentDir *= -1;

			ParametersHelper.setFieldValue(clonedBlock, "Direction", currentDir);

		} catch(BlockDefinitionException e) {
			// block doesn't have Direction parameter, ignore
		}

		return clonedBlock;
	}
}
