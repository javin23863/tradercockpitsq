/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * https://roadmap.strategyquant.com
 *
 * This code can be used only within StrategyQuant products.
 * Every owner of valid (free, trial or commercial) license of any StrategyQuant product
 * is allowed to freely use, copy, modify or make derivative work of this code without limitations,
 * to be used in all StrategyQuant products and share his/her modifications or derivative work
 * with the StrategyQuant community.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.Internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.IParametersHelperModifier;
import com.strategyquant.tradinglib.ParametersHelper;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;

/**
 * The Class FormulaBlock.
 */
abstract public class FormulaBlock extends StandardBlock implements IFormula {
	
	/** The Constant Log. */
	public static final Logger Log = LoggerFactory.getLogger("FormulaBlock");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * Evaluate block.
	 *
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	public double evaluateBlock() throws TradingException {
		throw new TradingException("This shouldn't be called for FormulaBlock!");
	}

	//------------------------------------------------------------------------

	/**
	 * Evaluate block.
	 *
	 * @param relativeShift the relative shift
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	public double evaluateBlock(int relativeShift) throws TradingException {
		throw new TradingException("This shouldn't be called for FormulaBlock!");
	}

	//------------------------------------------------------------------------

	/**
	 * New formula instance.
	 *
	 * @param strategy the strategy
	 * @param elFormulaBlock the el formula block
	 * @return the i formula
	 * @throws BlockDefinitionException the block definition exception
	 */
	public IFormula newFormulaInstance(StrategyBase strategy,  Element elFormulaBlock) throws BlockDefinitionException {
		try {
			Constructor<? extends FormulaBlock> constructor = null;

			constructor = this.getClass().getConstructor();

			FormulaBlock formula =  SQUtils.invokeUnchecked(constructor);

			// copy parameters from old block to new one
			ParametersHelper.copyParametersFromBlockToBlock(this, formula, new IParametersHelperModifier() {
				public Object modifyParameterValue(String paramName, Object existingParam) throws BlockDefinitionException {
					if(existingParam instanceof IBlock) {
						return ((IBlock) existingParam).clone(true, strategy);
					}
					
					return existingParam;
				}
			});
			
/*			
			for(String paramName : ParametersHelper.getParameters(this)) {
				Object paramValue = ParametersHelper.getParameterValue(this, paramName);
				if(paramValue instanceof IBlock) {
					paramValue = ((IBlock) paramValue).clone(true, strategy);
				}

				ParametersHelper.setParameterValue(formula, paramName, paramValue);
			}
*/
			
			formula.initialize(strategy, elFormulaBlock);
			
			return formula;

		} catch (NoSuchMethodException | SecurityException | BlockDefinitionException e) {
			throw new BlockDefinitionException("Exception cloning block! "+e.getMessage(), e);
		}
	}
	
/*
	public IFormula newFormulaInstanceOld(StrategyBase strategy,  Element elFormulaBlock) throws BlockDefinitionException {
		FormulaBlock formula = SQUtils.deepClone(this);
		
		formula.initialize(strategy, elFormulaBlock);
		
		return formula;
	}
*/
	//------------------------------------------------------------------------

	/**
 * Initialize.
 *
 * @param strategy the strategy
 * @param elFormulaBlock the el formula block
 * @throws BlockDefinitionException the block definition exception
 */
protected void initialize(StrategyBase strategy, Element elFormulaBlock) throws BlockDefinitionException {
		initializeStrategy(strategy);
		
		Class aClass = this.getClass();

		//Log.debug("Parsing XML - Formula '"+aClass.getSimpleName()+"'");
		
		for(Field field : aClass.getFields()) {
			AnnotationProcessor.wizardParseXml(this, field, elFormulaBlock);
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Initialize strategy.
	 *
	 * @param strategy the strategy
	 */
	private void initializeStrategy(StrategyBase strategy) {
		if(this.Strategy == null && this.nonXmlStrategy == null) {
			if(strategy != null) {
				if(strategy instanceof XmlStrategy) {
					this.Strategy = (XmlStrategy) strategy;
				} else {
					nonXmlStrategy = strategy;
				}
			}
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Checks if is none value.
	 *
	 * @return true, if is none value
	 */
	@Override
	public boolean isNoneValue() {
		Class aClass = this.getClass();

		Formula aFromula = (Formula) aClass.getAnnotation(Formula.class);

		if(aFromula != null) return aFromula.noneValue();
		
		return false;
	}	
	
	//------------------------------------------------------------------------

	/**
	 * Checks if is boolean value.
	 *
	 * @return true, if is boolean value
	 */
	@Override
	public boolean isBooleanValue() {
		return false;
	}
	
}
