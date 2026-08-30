package SQ.Negater;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

/**
 * special negater that negates AND / OR blocks by cloning them and negating its children
 */
@SortOrder(100)
public class AndOrNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negaters, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Comparison) {
			return null;
		}
		
		String blockName = block.getClass().getSimpleName();
		if(!blockName.equals("AND") && !blockName.equals("OR")) {
			return null;
		}
		
		// it is comparison
		IBlock clonedBlock = Blocks.get(blockName);
		
		// copy parameters from one block to another
		IBlock[] children = (IBlock[]) ParametersHelper.getParameterValue(block, "Children");
			
		IBlock[] clonedChildren = new IBlock[children.length];

		for(int i=0; i<children.length; i++) {
			clonedChildren[i] = negaters.negate(children[i], strategy);
		}

		ParametersHelper.setParameterValue(clonedBlock, "Children", clonedChildren);
		
		return clonedBlock;
	}
		
}
