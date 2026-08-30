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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.datalib.DataSeries;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;


/**
 * The Class StandardBlock.
 */
abstract class StandardBlock extends Debugger implements IBlock {
	
	/** The Constant Log. */
	public static final Logger Log = LoggerFactory.getLogger("StandardBlock");
	
	/** The Strategy. */
	public XmlStrategy Strategy;
	
	/** The non xml strategy. */
	protected StrategyBase nonXmlStrategy = null;
	
	/** The custom data. */
	private Int2IntOpenHashMap customData = null;
	
	/** The return type. */
	private int returnType = ReturnTypes.None;
	
	/** The exit methods class. */
	private final Class<?> exitMethodsClass = com.strategyquant.tradinglib.ExitMethod[].class;
	
	/** The chart index. */
	public int chartIndex;
	
	protected DataSeries barsShiftedSeries;
	
	protected int reservedBars = 0;
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * New instance.
	 *
	 * @param strategy the strategy
	 * @param elBlock the el block
	 * @return the i block
	 * @throws BlockDefinitionException the block definition exception
	 */
	public IBlock newInstance(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		StandardBlock block = (StandardBlock) clone(true, strategy);
		
		block.initialize(strategy, elBlock);
		
		return block;
	}

/*
	public IBlock newInstanceOld(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		StandardBlock block = SQUtils.deepClone(this);
		
		block.initialize(strategy, elBlock);
		
		return block;
	}
*/
	//------------------------------------------------------------------------

	/**
 * Clone.
 *
 * @param includingParameters the including parameters
 * @param strategy2 the strategy 2
 * @return the i block
 * @throws BlockDefinitionException the block definition exception
 */
public IBlock clone(boolean includingParameters, StrategyBase strategy2) throws BlockDefinitionException {
		try {
	        Constructor<? extends StandardBlock> constructor = null;

			constructor = this.getClass().getConstructor();

			StandardBlock newBlock =  SQUtils.invokeUnchecked(constructor);

			newBlock.initializeStrategy(strategy2);

			if(newBlock.nonXmlStrategy == null && this.nonXmlStrategy != null) {
				newBlock.nonXmlStrategy = this.nonXmlStrategy;
			}
	        
			StrategyBase strategy = this.getStrategy();
			
			if(includingParameters) {
				// copy parameters from old block to new one
				Class aClass = this.getClass();
				
				for(Field field : aClass.getFields()) {
					
					for(Annotation annotation : field.getAnnotations()) {
						if(annotation instanceof Parameter) {
							Object paramValue = field.get(this);
							
							if(paramValue instanceof IBlock) {
								paramValue = ((IBlock) paramValue).clone(true, strategy2);
							} else if(paramValue instanceof IFormula) {
								paramValue = ((IBlock) paramValue).clone(true, strategy2);
							} else {
								// it is normal parameter - number or string
								// check if this field is attached to a variable, if yes, attach also field of a new block to the same variable
								if(strategy != null) {
									Variable var = strategy.variables().getByField(this, field.getName());
							
									if(var != null) {
										var.registerAttachedField(newBlock, field);
										
										strategy.variables().getByField(newBlock, field.getName());
									}
								}
							}

							field.set(newBlock, paramValue);
							//ParametersHelper.setParameterValue(newBlock, field.getName(), paramValue);
							continue;
						}
					}
					
					if(field.getName().equals("ExitMethods")) {
						ExitMethod[] exitMethods = (ExitMethod[]) field.get(this);
						if(exitMethods != null) {
							// negate also exit methods
							ExitMethod[] clonedExitMethods = new ExitMethod[exitMethods.length];

							for(int i=0; i<exitMethods.length; i++) {
								Object paramValue = exitMethods[i];

								if(paramValue instanceof IFormula) {
									paramValue = ((IBlock) paramValue).clone(true, strategy2);

								} else if(paramValue instanceof ExitMethod) {
									continue;
									
								} else if(paramValue instanceof IBlock) {
									paramValue = ((IBlock) paramValue).clone(true, strategy2);

								} else {
									// it is normal parameter - number or string
									// check if this field is attached to a variable, if yes, attach also field of a new block to the same variable
									if(strategy != null) {
										Variable var = strategy.variables().getByField(this, field.getName());

										if(var != null) {
											var.registerAttachedField(newBlock, field);

											strategy.variables().getByField(newBlock, field.getName());
										}
									}
								}							
							}

							field.set(newBlock, clonedExitMethods);
							//ParametersHelper.setFieldValue(newBlock, field.getName(), clonedExitMethods);
						}
					}
				}
				
				if(this.customData != null) {
					newBlock.customData = new Int2IntOpenHashMap();
					newBlock.customData.putAll(this.customData);
				}
			}
			
	        return newBlock;
			
		} catch (NoSuchMethodException | SecurityException | BlockDefinitionException | IllegalArgumentException | IllegalAccessException e) {
			throw new BlockDefinitionException("Exception cloning block! "+e.getMessage(), e);
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
	public boolean isTradestationEngine() {
		//first check noXmlStrategy
		if (nonXmlStrategy != null) {
			return nonXmlStrategy.isTradestationEngine();
		}
		// then check xml strategy
		if (Strategy != null) {
			return Strategy.isTradestationEngine();
		}
		return false;
	}

	//------------------------------------------------------------------------

	/**
	 * Initialize.
	 *
	 * @param strategy the strategy
	 * @param elBlock the el block
	 * @throws BlockDefinitionException the block definition exception
	 */
	protected void initialize(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		initializeStrategy(strategy);
		
		if(elBlock != null) {
			try {
				parseXml(elBlock);
			} catch(NumberFormatException e) {
				Log.error("NumberFormatException, XML: {}", XMLUtil.elementToString(elBlock));
				
				throw e;
			}
		}
		
		OnInit();
	}

	//------------------------------------------------------------------------

	/**
	 * Deinitialize.
	 */
	public void deinitialize() {
		OnDeinit();
	}
	
	//------------------------------------------------------------------------

	/**
	 * On init.
	 *
	 * @throws BlockDefinitionException the block definition exception
	 */
	protected void OnInit() throws BlockDefinitionException {}

	//------------------------------------------------------------------------

	/**
	 * On deinit.
	 */
	protected void OnDeinit() {}
	
	//------------------------------------------------------------------------

	/**
	 * Gets the strategy.
	 *
	 * @return the strategy
	 */
	@Override
	public StrategyBase getStrategy() {
		//return Strategy;
		return (Strategy != null ? Strategy : nonXmlStrategy);
	}	

	//------------------------------------------------------------------------

	/**
	 * Parses the xml.
	 *
	 * @param elBlock the el block
	 * @throws BlockDefinitionException the block definition exception
	 */
	protected void parseXml(Element elBlock) throws BlockDefinitionException {
		// init all parameters of the given block and all sub-blocks
		Class aClass = this.getClass();

		for(Field field : aClass.getFields()) {
	
			//if(field.getType().toString().equals("class [Lcom.strategyquant.tradinglib.ExitMethod;")) {
			if(field.getType() == exitMethodsClass) {
				AnnotationProcessor.wizardParseExitTypesXml(this, field, elBlock);
				
				sortExitmethodsInArray(field);
			} else {
				AnnotationProcessor.wizardParseXml(this, field, elBlock);
			}
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Sort exitmethods in array.
	 *
	 * @param field the field
	 */
	private void sortExitmethodsInArray(Field field) {
		Object objExitMethods = null;
		try {
			objExitMethods = field.get(this);
		} catch (IllegalArgumentException e) {
			Log.info("Cannot sort exit methods (1)", e);
		} catch (IllegalAccessException e) {
			Log.info("Cannot sort exit methods (2)", e);
		}
		if(objExitMethods == null) {
			return;
		}
		
		ExitMethod[] exitMethods = (ExitMethod[]) objExitMethods;

		Blocks.sortByOrder(exitMethods);
	}

	//------------------------------------------------------------------------

	/**
	 * Negate.
	 *
	 * @return the i block
	 */
	public IBlock negate() {
		return this;
	}
	
	//------------------------------------------------------------------------

	/**
	 * Sets the custom data.
	 *
	 * @param key the key
	 * @param value the value
	 */
	@Override
	public void setCustomData(String key, int value) {
		if(customData == null) {
			customData = new Int2IntOpenHashMap();
		}
		
		customData.put(key.hashCode(), value);
		
		if(key.equals("StockpickerChartIndex")) {
			this.chartIndex = value;
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the custom data.
	 *
	 * @param key the key
	 * @return the custom data
	 */
	@Override
	public int getCustomData(String key) {
		if(customData == null || !customData.containsKey(key.hashCode())) {
			return Integer.MIN_VALUE;
		}
		
		return customData.get(key.hashCode());
	}	
	
	//------------------------------------------------------------------------

	/**
	 * Copy custom data.
	 *
	 * @param block the block
	 */
	@Override
	public void copyCustomData(IBlock block) {
		if(block instanceof StandardBlock) {
			StandardBlock sb = (StandardBlock) block;
			if(sb.customData != null) {
				if(customData  == null) {
					customData = new Int2IntOpenHashMap();
				}
				
				customData.putAll(sb.customData);
			}
		}
	}		

	//------------------------------------------------------------------------

	/**
	 * Gets the return type.
	 *
	 * @return the return type
	 */
	@Override
	public int getReturnType() {
		return returnType;
	}

	//------------------------------------------------------------------------

	/**
	 * Sets the return type.
	 *
	 * @param returnType the new return type
	 */
	@Override
	public void setReturnType(int returnType) {
		this.returnType = returnType;
	}
	
	//------------------------------------------------------------------------

	/**
	 * Gets the custom block xml.
	 *
	 * @param type the type
	 * @return the custom block xml
	 * @throws BlockDefinitionException the block definition exception
	 */
	@Override
	public Element getCustomBlockXml(int type) throws BlockDefinitionException {
		throw new BlockDefinitionException("Not implemented for this!");
	}		
}
