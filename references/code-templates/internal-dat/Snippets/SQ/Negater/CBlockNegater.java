package SQ.Negater;

import SQ.Blocks.Comparisons.LeftRightComparisonBlockAbstract;
import SQ.Internal.BlockUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.*;

import SQ.Internal.CBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SortOrder(1000)
public class CBlockNegater extends Negater {
	private static final Logger Log = LoggerFactory.getLogger("CBlockNegater");

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(!(block instanceof CBlock)) {
			return null;
		}
		
		//String origBlock = XMLUtil.elementToString(((CBlock) block).getCustomBlockXml(1));
		CBlock originalBlock = (CBlock) block;
		CBlock oppositeBlock = originalBlock.getOppositeBlock();

		IBlock contentsOrigBlock = originalBlock.getContents();
		IBlock contentsOppositeBlock = oppositeBlock.getContents();

		//String oppBlock = XMLUtil.elementToString(oppositeBlock.getCustomBlockXml(1));

		if(BlockUtils.isLeftRightBlock(contentsOrigBlock) && BlockUtils.isLeftRightBlock(contentsOppositeBlock)) {
			// it is <,>,<=,>=,=,<> block with Left and Right part
			// check if it isn't comparing oscillator - we'd have to change the Level value
			LeftRightComparisonBlockAbstract lrOppositeBlock = (LeftRightComparisonBlockAbstract) contentsOppositeBlock;

			// check if there is oscillator and number in the comparison
			if(isNumber(lrOppositeBlock.Left) && isOscillatorIndicator(lrOppositeBlock.Right)) {
				// block is oscillator, work with oscillation center in the condition

				double varValue = getNumber(lrOppositeBlock.Left);
				IBlock blockOscillator = lrOppositeBlock.Right;

				// change number to be centered by oscillator centre
				double middleValue = getOscillatorMiddleValue(blockOscillator);

				double diff = (middleValue - varValue);

				//now set the number value
				double prevValue = varValue;
				double currValue = middleValue + diff;

				// apply the new value to both block object and XML
				applyBlockObjChange(lrOppositeBlock.Left, currValue);
				oppositeBlock.applyParamChange(prevValue, currValue);

			} else if(isOscillatorIndicator(lrOppositeBlock.Left) && (isNumber(lrOppositeBlock.Right))) {
				// block is oscillator, work with oscillation center in the condition

				double varValue = getNumber(lrOppositeBlock.Right);
				IBlock blockOscillator = lrOppositeBlock.Left;

				// change number to be centered by oscillator centre
				double middleValue = getOscillatorMiddleValue(blockOscillator);

				double diff = (middleValue - varValue);

				//now set the number value
				double prevValue = varValue;
				double currValue = middleValue + diff;

				// apply the new value to both block object and XML
				applyBlockObjChange(lrOppositeBlock.Right, currValue);
				oppositeBlock.applyParamChange(prevValue, currValue);
			}
		}

		return oppositeBlock;
	}

	//------------------------------------------------------------------------
	private void applyBlockObjChange(IBlock block, double value) {
		if(block instanceof SQ.Blocks.Other.Number) {
			SQ.Blocks.Other.Number blockVar = (SQ.Blocks.Other.Number) block;
			blockVar.Number = value;
		}

		if(block instanceof SQ.Blocks.Other.IntVariable) {
			SQ.Blocks.Other.IntVariable blockVar = (SQ.Blocks.Other.IntVariable) block;
			blockVar.Variable = (int) value;
		}

		if(block instanceof SQ.Blocks.Other.DoubleVariable) {
			SQ.Blocks.Other.DoubleVariable blockVar = (SQ.Blocks.Other.DoubleVariable) block;
			blockVar.Variable = value;
		}
	}

	//------------------------------------------------------------------------

	private double getNumber(IBlock block) {
		if(block instanceof SQ.Blocks.Other.Number) {
			SQ.Blocks.Other.Number blockVar = (SQ.Blocks.Other.Number) block;
			return blockVar.Number;
		}

		if(block instanceof SQ.Blocks.Other.IntVariable) {
			SQ.Blocks.Other.IntVariable blockVar = (SQ.Blocks.Other.IntVariable) block;
			return blockVar.Variable;
		}

		if(block instanceof SQ.Blocks.Other.DoubleVariable) {
			SQ.Blocks.Other.DoubleVariable blockVar = (SQ.Blocks.Other.DoubleVariable) block;
			return blockVar.Variable;
		}

		return -1;
	}

	//------------------------------------------------------------------------

	private boolean isNumber(IBlock block) {
		if(block instanceof SQ.Blocks.Other.Number || block instanceof SQ.Blocks.Other.IntVariable || block instanceof SQ.Blocks.Other.DoubleVariable) {
			return true;
		}

		return false;
	}

}
