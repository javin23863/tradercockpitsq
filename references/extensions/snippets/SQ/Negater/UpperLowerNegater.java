package SQ.Negater;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.jdom2.Element;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(10100)
public class UpperLowerNegater extends Negater {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		
		if(!containsUpperLower(block)) {
			return null;
		}
		
		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		// copy parameters from one block to another
		ParametersHelper.negateParametersInClonedBlock(block, oppositeBlock, negatersList, strategy);
		
		// specially change outputIndex parameter
		Field outputIndexField = getOutputIndexField(block);
		Field outputIndexOppositeField = getOutputIndexField(oppositeBlock);
		
		if(outputIndexField == null || outputIndexOppositeField == null) {
			return null;
		}
		
		try {
			int outputIndex = outputIndexField.getInt(block);
			
			int oppositeOutputIndex = (outputIndex == 0 ? 1 : 0);
			
			outputIndexOppositeField.set(oppositeBlock, oppositeOutputIndex);
			
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new BlockDefinitionException("Exception setting value of parameter OutputIndex found!", e);
		}

		ParametersHelper.negateDataSeries(block, oppositeBlock);
		
		return oppositeBlock;		
	}

	//------------------------------------------------------------------------

	public boolean containsUpperLower(IBlock block) {
		ArrayList<String> parameters = new ArrayList<String>();
		
		int outputCounts = 0;
		Class aClass = block.getClass();
		
		for(Field field : aClass.getFields()) {
			for(Annotation annotation : field.getAnnotations()) {
				if(annotation instanceof Output) {
					if(field.getName().equals("Upper") || field.getName().equals("Lower")) {
						outputCounts++;
					}
				}
			}
		}
		
		// returns true only if it has exactly Upper and Lower @Output values
		return (outputCounts == 2);
	}
}
