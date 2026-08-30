package SQ.Negater;

import com.strategyquant.lib.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@SortOrder(10000)
public class ConditionsNegater extends Negater {
	private static final Logger Log = LoggerFactory.getLogger("ConditionsNegater");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public IBlock negate(NegatersList negatersList, IBlock block, int blockType, int returnType, StrategyBase strategy) throws BlockDefinitionException {
		if(blockType != BlockSuperTypes.Condition) {
			return null;
		}
		
		// it is comparison
		IBlock oppositeBlock = Blocks.getOppositeBlock(block);
		
		String fieldName = null;
		Object fieldValue = 0;
		Object paramValue;
		double tempValue = 0;
		
		OppositeBlock aOppositeBlock = block.getClass().getAnnotation(OppositeBlock.class);
		if(aOppositeBlock != null) {
			String oppositeBlockName = aOppositeBlock.value();
			
			if(aOppositeBlock.oscillator() == true) {
				// special handling for oscillators
				// this signal is based on oscillator indicator, we have to modify its comparison value to reflect this
				// Example: RSI oscillates around 50, hence "RSI > 30" -> "RSI < 70"
				if(oppositeBlockName.equals( oppositeBlock.getClass().getSimpleName() )) {
					
					double middleValue = aOppositeBlock.middleValue();

					try {
						paramValue = ParametersHelper.getParameterValue(block, aOppositeBlock.field());
						if(paramValue instanceof Double) {
							tempValue = (double) paramValue;
							fieldName = aOppositeBlock.field();

						} else if(paramValue instanceof Integer) {
							tempValue = (int) (paramValue);
							fieldName = aOppositeBlock.field();
						}
						
						if(fieldName != null) {
							double diff = (middleValue - tempValue);
							
							tempValue = middleValue + diff;

							if(paramValue instanceof Double) {
								fieldValue = new Double(tempValue);

							} else if(paramValue instanceof Integer) {
								fieldValue = new Integer((int) tempValue);
							}
						}
					} catch(BlockDefinitionException e) {

						Log.error("Cannot find field {} in block {}", aOppositeBlock.field(), block.getClass().getSimpleName());
					}
				}
			}
		}


		// copy parameters from one block to another
		Class aClass = oppositeBlock.getClass();
		
		for(Field field : aClass.getFields()) {
			for(Annotation annotation : field.getAnnotations()) {
				if(annotation instanceof Parameter) {

					String paramName = field.getName();
					
					if(fieldName != null && paramName.equals(fieldName)) {
						// block is based on oscillator
						// set correct value for opposite block 
						paramValue = fieldValue;

					} else {
						// block is based on normal indicator, or this parameter is not oscillator value field 
						paramValue = ParametersHelper.getParameterValue(block, paramName);

						if(paramValue instanceof IBlock) {
							paramValue = ((IBlock) paramValue).clone(true, strategy);
						}
					}
				
					try {
						field.set(oppositeBlock, paramValue);
						
						// check if original field wasn't a variable
						StrategyBase str = block.getStrategy();
						if(str != null) {
							Variables vars = str.variables();
							if(vars != null) {
								Variable var = vars.getByField(block, paramName);
								if(var != null) {
									var.registerAttachedField(oppositeBlock, field);
								}
							}
						}
						
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new BlockDefinitionException("Exception getting value of parameter '"+field.getName()+"' found!", e);
					}					
				}
			}
		}

		ParametersHelper.negateDataSeries(block, oppositeBlock);

		return oppositeBlock;
	}
		
}
