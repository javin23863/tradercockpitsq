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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import com.strategyquant.tradinglib.*;
import org.jdom2.Element;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.blocks.AnnotationProcessor;
import com.strategyquant.tradinglib.indicator.IndicatorBase;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * The Class IndicatorBlock.
 */
abstract public class IndicatorBlock extends Indicator implements IBlock {

	/** The Strategy. */
	public XmlStrategy Strategy = null;
	
	/** The non xml strategy. */
	private StrategyBase nonXmlStrategy = null;
	
	/** The Output index. */
	public int OutputIndex = 0;

	/** The custom data. */
	private Int2IntOpenHashMap customData = null;
	
	/** The writer. */
	private PrintWriter writer;
	
	/** The time formatter. */
	private DateTimeFormatter timeFormatter;
	
	/** The return type. */
	private int returnType = ReturnTypes.None;
	
	/** The indicator obj. */
	IndicatorBase indicatorObj = null;
	
	/** The outputs. */
	private DataSeries[] outputs;
	
	/** The no shift recognized. */
	private int noShiftRecognized = -1;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * On block evaluate.
	 *
	 * @param relativeShift the relative shift
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		
		try {

			if(indicatorObj == null) {
				initIndicatorObject(this.Strategy);

			} else {
				indicatorObj.refreshShift();
			}
						
			if(OutputIndex >= outputs.length) {
				throw new Exception("Invalid output index: " + OutputIndex+" in block "+this.getName());
			}

			int shift = relativeShift + Shift;
			
			if(noShiftRecognized == -1) {
				if(indicatorObj.getClass().isAnnotationPresent(NoShift.class)) {
					noShiftRecognized = 1;
				} else {
					noShiftRecognized = 0;
				}
			}
			
			if(noShiftRecognized == 1) {
				shift = 0;
			}
			
			return outputs[OutputIndex].get(shift);
			
		} catch(Exception e) {
			if(e.getMessage() != null && !e.getMessage().contains("Invalid output index:")) {
				Log.error("Indicator["+this.getClass().getSimpleName()+"] - " + "Indicator.OnBlockEvaluate() failed. Exc.", e);
			}
			throw new TradingException("Indicator["+this.getClass().getSimpleName()+"] - " + e.getMessage());
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Inits the indicator object.
	 *
	 * @throws Exception the exception
	 */
	private void initIndicatorObject(StrategyBase strategy) throws Exception {

		ObjectArrayList<Field> params = new ObjectArrayList<Field>();
		ObjectArrayList<String> outputObjects = new ObjectArrayList<String>();
		
		for(Field field : this.getClass().getFields()) {
			for(Annotation annotation : field.getAnnotations()) {
				if(annotation instanceof Parameter && !field.getName().equals("Shift")) {
					params.add(field);
					
				} else if(annotation instanceof Output) {
					outputObjects.add(field.getName());
				}
			}
		}			
		
		Class[] paramTypes = new Class[params.size()];
		for(int i=0; i<params.size(); i++) {
			paramTypes[i] = params.get(i).getType();
		}
		
		Method indyMethod = Indicators.getClass().getMethod(this.getClass().getSimpleName(), paramTypes);
		
		Object[] values = new Object[params.size()];
		for(int i=0; i<params.size(); i++) {
			values[i] = (Object) params.get(i).get(this);
		}
		
		Object indy = null;

		try {
			indy = indyMethod.invoke(Indicators, values);
			indicatorObj = (IndicatorBase) indy;
			
		} catch(java.lang.reflect.InvocationTargetException e) {

			if(e.getTargetException() != null) {
				String indicator = createIndicatorDescription(values);

				throw new Exception("Cannot create indicator "+indicator, e.getTargetException());
			}
			throw e;
		}
		
		outputs = new DataSeries[outputObjects.size()];
		
		for(int i=0; i<outputObjects.size(); i++) {
			outputs[i] = (DataSeries) indy.getClass().getField(outputObjects.get(i)).get(indy);
		}

		indicatorObj.initializeStrategy(strategy);
	}

	//------------------------------------------------------------------------

	private String createIndicatorDescription(Object[] values) {
		StringBuffer sb = new StringBuffer(this.getClass().getSimpleName());
		sb.append("(");

		for(int i=0; i<values.length; i++) {
			Object o = values[i];

			if(i>0) {
				sb.append(",");
			}

			if(o instanceof DataSeries) {
				sb.append("[DataSeries]");
			} else if(o instanceof ChartData) {
				sb.append("[ChartData]");
			} else if(o instanceof Integer) {
				sb.append(((Integer) o).toString());
			} else if(o instanceof Double) {
				sb.append(((Double) o).toString());
			} else {
				sb.append("[");
				sb.append(o.getClass().getSimpleName());
				sb.append("]");
			}
		}
		sb.append(")");

		return sb.toString();
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the super type.
	 *
	 * @return the super type
	 */
	public int getSuperType() {
		return BlockSuperTypes.Value;
	}
	
	//------------------------------------------------------------------------

	/**
	 * Evaluate block.
	 *
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	public double evaluateBlock() throws TradingException {
		double val = this.OnBlockEvaluate(0);

		return SQUtils.round6(val);
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
		return SQUtils.round6(this.OnBlockEvaluate(relativeShift));
	}

	//------------------------------------------------------------------------

	/**
	 * New instance.
	 *
	 * @param strategy the strategy
	 * @param elBlock the el block
	 * @return the i block
	 * @throws BlockDefinitionException the block definition exception
	 */
	@Override
	public IBlock newInstance(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		IndicatorBlock block = (IndicatorBlock) clone(true, strategy);
		
		block.initialize(strategy, elBlock);
		
		return block;
	}
	
/*
	public IBlock newInstanceOld(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		IndicatorBlock block = SQUtils.deepClone(this);
		
		block.initialize(strategy, elBlock);
		
		return block;
	}
*/	
	//------------------------------------------------------------------------

	/**
 * Clone.
 *
 * @param includingParameters the including parameters
 * @param strategy the strategy
 * @return the i block
 * @throws BlockDefinitionException the block definition exception
 */
public IBlock clone(boolean includingParameters, StrategyBase strategy) throws BlockDefinitionException {
		try {
	        Constructor<? extends IndicatorBlock> constructor = null;

			constructor = this.getClass().getConstructor();

			IndicatorBlock newBlock =  SQUtils.invokeUnchecked(constructor);
			
			newBlock.initializeStrategy(strategy);
			
			if(includingParameters) {
				// copy parameters from old block to new one
				ParametersHelper.copyParametersFromBlockToBlock(this, newBlock, new IParametersHelperModifier() {
					
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

					ParametersHelper.setParameterValue(newBlock, paramName, paramValue);
				}
*/				
				if(this.customData != null) {
					newBlock.customData = new Int2IntOpenHashMap();
					newBlock.customData.putAll(this.customData);
					newBlock.OutputIndex = this.OutputIndex;
				}				
			}
			
	        return newBlock;
			
		} catch (NoSuchMethodException | SecurityException | BlockDefinitionException e) {
			throw new BlockDefinitionException("Exception cloning block! "+e.getMessage(), e);
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Initialize.
	 *
	 * @param strategy the strategy
	 * @param elBlock the el block
	 * @throws BlockDefinitionException the block definition exception
	 */
	private void initialize(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		initializeStrategy(strategy);
		
		if(Strategy != null) {
			this.Indicators = Strategy.Indicators;
		}
		
		if(elBlock != null) {
			parseXml(elBlock);
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Initialize strategy.
	 *
	 * @param strategy the strategy
	 */
	public void initializeStrategy(StrategyBase strategy) {
		if(strategy == null) {
			return;
		}

		if(this.Strategy == null && this.nonXmlStrategy == null) {
			if(strategy != null) {
				if(strategy instanceof XmlStrategy) {
					this.Strategy = (XmlStrategy) strategy;
				} else {
					nonXmlStrategy = strategy;
				}
			}
		}

		super.initializeStrategy(strategy);
	}
	
	//------------------------------------------------------------------------

	/**
	 * Gets the strategy.
	 *
	 * @return the strategy
	 */
	@Override
	public StrategyBase getStrategy() {
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
		
		//Log.debug("Parsing indicator XML - Block '"+aClass.getSimpleName()+"'");
		
		Annotation aNoShift = aClass.getAnnotation(NoShift.class);

		for(Field field : aClass.getFields()) {
			if(field.getName().equals("Shift") && aNoShift != null) {
				// this indicator doesn't use Shift parameter
				continue;
			}
			
			AnnotationProcessor.wizardParseXml(this, field, elBlock);
		}
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
		if(customData  == null) {
			customData = new Int2IntOpenHashMap();
		}
		
		customData.put(key.hashCode(), value);
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
		if(block instanceof IndicatorBlock) {
			IndicatorBlock ib = (IndicatorBlock) block;
			if(ib.customData != null) {
				if(customData  == null) {
					customData = new Int2IntOpenHashMap();
				}
				
				customData.putAll(ib.customData);
			}
			
			this.OutputIndex = ib.OutputIndex;
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Inits the logging.
	 *
	 * @param fileName the file name
	 * @throws TradingException the trading exception
	 */
	protected void initLogging(String fileName) throws TradingException {
		timeFormatter = DateTimeFormat.forPattern("yyyy.MM.dd,HH:mm");
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(fileName), StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new TradingException(e.getMessage());
		}
	}
	
	//------------------------------------------------------------------------

	/**
	 * Log to file.
	 *
	 * @param text the text
	 */
	protected void logToFile(String text) {
		if(writer == null) return;
		
		writer.write(text);
	}
	
	//------------------------------------------------------------------------

	/**
	 * Finish logging.
	 */
	protected void finishLogging() {
		if(writer != null) {
			writer.close();
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

	//------------------------------------------------------------------------

	public int getCurrentBar() {
		if(Strategy.isTradestationEngine()) {
			return CurrentBar - this.getIndyStartingBar();
		} else {
			return CurrentBar;
		}
	}
}
