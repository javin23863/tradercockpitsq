package SQ.Negater;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;

import SQ.Blocks.Indicators.Fibo.Fibo;

@SortOrder(900000)
public class PivotsNegater extends Negater {
	public static final Logger Log = LoggerFactory.getLogger("PivotsNegater");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		if(!block.getClass().getSimpleName().equals("Pivots")) {
			// it is not Fibo block, skip
			return null;
		}

		// standard negation first
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);
		ParametersHelper.negateDataSeries(block, oppositeBlock);

		// now special Pivots negation
		Field pivotsLineField = getOutputIndexField(block);
		Field oppositePivotsLineField = getOutputIndexField(oppositeBlock);
		
		if(pivotsLineField == null || oppositePivotsLineField == null) {
			return null;
		}
		
		try {
			int pivotLine = pivotsLineField.getInt(block);
			int oppositePivotLine = 0;

			/*
				pivot line 0 (Pivot) should negate to pivot line 0 (Pivot)
				pivot line 1 (R1) should negate to pivot line 4 (S1)
				pivot line 2 (R2) should negate to pivot line 5 (S2)
				pivot line 3 (R3) should negate to pivot line 6 (S3)
				pivot line 4 (S1) should negate to pivot line 1 (R1)
				pivot line 5 (S2) should negate to pivot line 2 (R2)
				pivot line 6 (S3) should negate to pivot line 3 (R3)
			 */	
			
			switch(pivotLine) {
				case 0: oppositePivotLine = 0; break; // Pivot
				case 1: oppositePivotLine = 4; break; // R1
				case 2: oppositePivotLine = 5; break; // R2
				case 3: oppositePivotLine = 6; break; // R3
				case 4: oppositePivotLine = 1; break; // S1
				case 5: oppositePivotLine = 2; break; // S2
				case 6: oppositePivotLine = 3; break; // S3
			}

			oppositePivotsLineField.set(oppositeBlock, oppositePivotLine);		

		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new BlockDefinitionException("Exception setting value of parameter OutputIndex found!", e);
		}


		return oppositeBlock;		
	}
}
