package SQ.Negater;

import SQ.Blocks.Comparisons.LeftRightComparisonBlockAbstract;
import SQ.Internal.BlockUtils;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(1000)
public class OscillatorComparisonsNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Comparison) {
			return null;
		}
		
		if(!BlockUtils.isLeftRightBlock(block)) {
			return null;
		}
		
		// it is comparison
		IBlock oppositeComparison = Blocks.getOppositeBlock(block);
		
		// it is <,>,<=,>=,=,<> block with Left and Right part
		if(!BlockUtils.isLeftRightBlock(oppositeComparison)) {
			return null;
		}
			
		LeftRightComparisonBlockAbstract lrBlock = (LeftRightComparisonBlockAbstract) block;
		LeftRightComparisonBlockAbstract lrOppositeBlock = (LeftRightComparisonBlockAbstract) oppositeComparison;
			
		// check if there is oscillator and number in the comparison
		if((lrBlock.Left instanceof SQ.Blocks.Other.Number) && isOscillatorIndicator(lrBlock.Right)) {
			// block is oscillator, work with oscillation center in the condition

			lrOppositeBlock.Left = lrBlock.Left.clone(true, strategy);
			lrOppositeBlock.Right = lrBlock.Right.clone(true, strategy);
			
			SQ.Blocks.Other.Number blockNumber = (SQ.Blocks.Other.Number) lrOppositeBlock.Left;
			IBlock blockOscillator = lrBlock.Right;
			
			// change number to be centered by oscillator centre
			double middleValue = getOscillatorMiddleValue(blockOscillator);

			double diff = (middleValue - blockNumber.Number);

			//now set the number value
			blockNumber.Number = middleValue + diff;


		} else if(isOscillatorIndicator(lrBlock.Left) && (lrBlock.Right instanceof SQ.Blocks.Other.Number)) {
			// block is oscillator, work with oscillation center in the condition

			lrOppositeBlock.Left = lrBlock.Left.clone(true, strategy);
			lrOppositeBlock.Right = lrBlock.Right.clone(true, strategy);
			
			SQ.Blocks.Other.Number blockNumber = (SQ.Blocks.Other.Number) lrOppositeBlock.Right;
			IBlock blockOscillator = lrBlock.Left;
			
			// change number to be centered by oscillator centre
			double middleValue = getOscillatorMiddleValue(blockOscillator);

			double diff = (middleValue - blockNumber.Number);

			//now set the number value
			blockNumber.Number = middleValue + diff;

		} else {
			// this comparison doesn't compare oscillator indicator and a number
			// handle it in another negater
			return null;
		}

		ParametersHelper.negateDataSeries(block, lrOppositeBlock);

		return lrOppositeBlock;

		// it is another block with different number of parameters
		//for(Object param : getParameters(oppositeBlock)) {
		//		setParameter(oppositeBlock, param, getParameter(block, param));
		//}
	}
	
	
		
}
