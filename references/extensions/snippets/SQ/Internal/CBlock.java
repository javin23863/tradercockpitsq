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

import java.util.HashMap;
import java.util.List;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.CustomBlocks;

/**
 * The Class CBlock.
 */
public class CBlock extends StandardBlock {

	/** The Contents. */
	@Parameter
	public IBlock Contents;
	
	/** The el block. */
	private Element elBlock;

	/** The cb key. */
	private String cbKey;
	
	/** The cb params. */
	private HashMap<String, CParamType> cbParams = null;


	/**
	 * The Class CParamType.
	 */
	class CParamType {
		
		/** The value. */
		String value = null;
		
		/** The variable. */
		String variable = null;
		
		/** The variable type. */
		String variableType = null;

		/**
		 * Instantiates a new c param type.
		 *
		 * @param elParam the el param
		 */
		public CParamType(Element elParam) {
			value = elParam.getText();
			variable = elParam.getAttributeValue("variable");
			variableType = elParam.getAttributeValue("variableType");
			
			//handling chart params
			String type = elParam.getAttributeValue("type");
			
			if(variableType == null && type != null && type.equals("data")) {
				variableType = "data";
			}
		}
	}
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	/**
	 * Evaluate block.
	 *
	 * @param relativeShift the relative shift
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	@Override
	public double evaluateBlock(int relativeShift) throws TradingException {
		if(Contents == null) {
			Log.error("Cannot evaluate custom block, it has no contents!");
			return 0;
		}
		
		if(Contents instanceof ComparisonBlock) {
			boolean retval = ((ComparisonBlock) Contents).OnEvaluateComparison();
			
			return (retval ? 1 : 0);
			
		} else if(Contents instanceof ConditionBlock) {
			boolean retval = ((ConditionBlock) Contents).OnBlockEvaluate();
			
			return (retval ? 1 : 0);
		}

		
		return Contents.evaluateBlock(relativeShift);
	}

	//------------------------------------------------------------------------

	/**
	 * Evaluate block.
	 *
	 * @return the double
	 * @throws TradingException the trading exception
	 */
	@Override
	public double evaluateBlock() throws TradingException {
		return evaluateBlock(0);
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
	public IBlock newInstance(StrategyBase strategy, Element elBlock) throws BlockDefinitionException {
		CBlock block = (CBlock) clone(true, strategy);
		
		block.elBlock = elBlock.clone();
		
		block.initialize(strategy, elBlock);
		
		if(strategy instanceof XmlStrategy) {
			block.Strategy = (XmlStrategy) strategy;
		}
		
		return block;
	}

	//------------------------------------------------------------------------

	/**
	 * Initialize CB params.
	 *
	 * @param elBlock the el block
	 */
	private void initializeCBParams(Element elBlock) {
		cbParams = new HashMap<String, CParamType>();

		List<Element> children = elBlock.getChildren("Param");
		for(int i=0; i<children.size(); i++) {
			Element elParam = children.get(i);
			
			String key = elParam.getAttributeValue("key");
			CParamType val = new CParamType(elParam);
			
			cbParams.put(key, val);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Parses the xml.
	 *
	 * @param elBlock the el block
	 * @throws BlockDefinitionException the block definition exception
	 */
	protected void parseXml(Element elBlock) throws BlockDefinitionException {
		cbKey = elBlock.getAttributeValue("key");
		
		initializeCBParams(elBlock);

		
		StrategyBase strategy = getStrategy();
		
		Element elCustomBlock = CustomBlocks.getElement(cbKey);
		if(elCustomBlock != null && strategy != null) {
			elCustomBlock = elCustomBlock.clone();
		}
		
		if(elCustomBlock == null) {
			throw new BlockDefinitionException("Custom block '"+cbKey+"' not found in map!");
		}
		
		if(this.elBlock == null) {
			this.elBlock = elCustomBlock;
		}

		Element elBlockContents = elCustomBlock.getChild("Contents").getChild("Item");
		if(elBlockContents == null) {
			throw new BlockDefinitionException("Custom block '"+cbKey+"' has no contents!");
		}
		
		//String str = XMLUtil.elementToString(elBlock);
		//String str2 = XMLUtil.elementToString(elBlockContents);
				
		applyActualParameters(elBlockContents);

		//String str3 = XMLUtil.elementToString(elBlockContents);
		
		String realBlockKey = elBlockContents.getAttributeValue("key"); 
		
		Contents = Blocks.getBlockObject(realBlockKey, strategy, elBlockContents);

	}
	
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
		CBlock clonedBlock = (CBlock) super.clone(includingParameters, strategy);
		
		clonedBlock.cbKey = this.cbKey;
		if(elBlock != null) {
			clonedBlock.elBlock = this.elBlock.clone();
		}
		
		if(this.cbParams != null) {
			clonedBlock.cbParams = new HashMap<String, CParamType>(this.cbParams);
		}
		
		return clonedBlock;
	}
	
	//------------------------------------------------------------------------

	/*
elBlock:
<Item key="CBlock_Xlose321" name="Xlose 321" display="Xlose 321" ignoreInBuilder="false" returnType="boolean" categoryType="custom blocks">
  <Param key="#C2C55B8CA5555887A24482FC930BFED2#" name="Chart" type="data" controlType="dataVar" defaultValue="0" minValue="" maxValue="" step="" builderStep="1" values="">0</Param>
</Item>

elBlockContents
<Item key="CBlock_Xlose321" name="Xlose 321" display="Xlose 321" type="Condition" returnType="boolean" oppositeBlockKey="CBlock_Xlose123">
  <Param key="C2C55B8CA5555887A24482FC930BFED2" name="Chart" display="Chart" type="data" controlType="dataVar" defaultValue="0" />
  <Contents>
    <Item key="IsGreater" name="(&gt;) Is greater" display="#Left# &gt; #Right#" mI="Comparisons" returnType="boolean" categoryType="operators">
      <Block key="#Left#">
        <Item key="Close" name="(C) Close" display="Close[#Shift#]" mI="Price" returnType="price" categoryType="priceValue">
          <Param key="#Chart#" name="Chart" type="data" controlType="dataVar" defaultValue="0" customParam="true">C2C55B8CA5555887A24482FC930BFED2</Param>
          <Param key="#Shift#" name="Shift" type="int" defaultValue="1" controlType="jspinnerVar" minValue="0" maxValue="1000" genMinValue="-1000001" genMaxValue="-1000002" paramType="shift" step="1" builderStep="1">3</Param>
        </Item>
      </Block>
      <Block key="#Right#">
        <Item key="Close" name="(C) Close" display="Close[#Shift#]" mI="Price" returnType="price" categoryType="priceValue">
          <Param key="#Chart#" name="Chart" type="data" controlType="dataVar" defaultValue="0" customParam="true">C2C55B8CA5555887A24482FC930BFED2</Param>
          <Param key="#Shift#" name="Shift" type="int" defaultValue="1" controlType="jspinnerVar" minValue="0" maxValue="1000" genMinValue="-1000001" genMaxValue="-1000002" paramType="shift" step="1" builderStep="1">2</Param>
        </Item>
      </Block>
    </Item>
  </Contents>
</Item>

	 */

	//------------------------------------------------------------------------
	
	/**
	 * Apply actual parameters.
	 *
	 * @param elBlockContents the el block contents
	 * @throws BlockDefinitionException the block definition exception
	 */
	private void applyActualParameters(Element elBlockContents) throws BlockDefinitionException {
		if(cbParams != null) {
			replaceParameterWithValue(elBlockContents);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Replace parameter with value.
	 *
	 * @param elParent the el parent
	 */
	private void replaceParameterWithValue(Element elParent) {
		if(elParent.getName().equals("Param")) {
			String potentialKey = elParent.getText();
			
			CParamType realValue = cbParams.get(potentialKey);
			if(realValue != null && realValue.value != null) {
				// replace parameter key with real value
				
				elParent.setText(realValue.value);
				
				if(realValue.variable != null) {
					elParent.setAttribute("variable", realValue.variable);
				}

				if(realValue.variableType != null) {
					elParent.setAttribute("variableType", realValue.variableType);
					
					//Try convert chart parameter to symbol
					
					String parentKey = elParent.getAttributeValue("key");
					
					if(realValue.variableType.equals("data") && parentKey != null && parentKey.equals("#Symbol#")) {
						int valueInt = -1;
						try { valueInt = Integer.parseInt(realValue.value); } catch(Exception e){}
						
						if(valueInt >= 0 && Strategy != null) {
							elParent.setText(Strategy.Symbol);
						}
					}
				}
			}
			
			elParent.removeAttribute("customParam");
		}
		
		List<Element> children = elParent.getChildren();
		for(int i=0; i<children.size(); i++) {
			replaceParameterWithValue(children.get(i));
		}
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
		Element elBlockXML;

		if(cbKey == null) {
			throw new BlockDefinitionException("Custom block key doesn't exist!");
		}

		if(type == 3) {
			return CustomBlocks.getElement(cbKey);
		}

		if(elBlock != null) {
			elBlockXML = elBlock;
		} else { 
			elBlockXML = CustomBlocks.getElement(cbKey);
		}

		if(type == 0) {
			// return complete XML of the custom block
			return elBlockXML.clone();
			
		} else {
			// used for generating strategy XML code
			elBlockXML = elBlockXML.clone();
			elBlockXML.removeChild("Contents");
			
			return elBlockXML;
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the opposite block.
	 *
	 * @return the opposite block
	 * @throws BlockDefinitionException the block definition exception
	 */
	public CBlock getOppositeBlock() throws BlockDefinitionException {
		if(cbKey == null) {
			throw new BlockDefinitionException("Custom block key doesn't exist!");
		}

		Element elActualBlockDef = null;
		
		if(elBlock == null) {
			elBlock = CustomBlocks.getElement(cbKey);
			if(elBlock == null) {
				throw new BlockDefinitionException("Custom block '"+cbKey+"' element not found in map!");
			}

			elActualBlockDef = elBlock;
			elBlock = elBlock.clone();
		}

		// get actual opposite block
		if(elActualBlockDef == null) {
			elActualBlockDef = CustomBlocks.getElement(cbKey);
		}
		if(elActualBlockDef == null) {
			throw new BlockDefinitionException("Custom block '"+cbKey+"' element not found in map!");
		}
  
		String oppositeBlockKey = elActualBlockDef.getAttributeValue("oppositeBlockKey");
		if(oppositeBlockKey == null || oppositeBlockKey.equals("") || oppositeBlockKey.equals("CBlock_null") || oppositeBlockKey.equals("null") || oppositeBlockKey.equals("CBlock_")) {
			oppositeBlockKey = cbKey;
		}
		
		Element elOppositeBlock = CustomBlocks.getElement(oppositeBlockKey);
		if(elOppositeBlock == null) {
			throw new BlockDefinitionException("Custom (opposite) block '"+oppositeBlockKey+"' element not found in map!");
		}
		
		elOppositeBlock = elOppositeBlock.clone();
		
		// opposite block doesn't have its Custom block parameters initialized, do it from this block
		applyCBParams(elOppositeBlock);
		fixEmptyCBParams(elOppositeBlock);

		CBlock oppositeBlock = new CBlock();
		
		oppositeBlock.elBlock = elOppositeBlock;
		
		oppositeBlock.initialize(getStrategy(), elOppositeBlock);
		
		return oppositeBlock;
	}

	//------------------------------------------------------------------------

	/**
	 * Apply CB params.
	 *
	 * @param elBlock the el block
	 */
	private void applyCBParams(Element elBlock) {
		if(cbParams == null) {
			return;
		}

		List<Element> children = elBlock.getChildren("Param");
		for(int i=0; i<children.size(); i++) {
			Element elParam = children.get(i);
			
			String key = elParam.getAttributeValue("key");
		
			CParamType realValue = cbParams.get(key);
			if(realValue != null && realValue.value != null) {
				elParam.setText(realValue.value);
				
				if(realValue.variable != null) {
					elParam.setAttribute("variable", realValue.variable);
				}

				if(realValue.variableType != null) {
					elParam.setAttribute("variableType", realValue.variableType);
				}				
			}
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Apply CB params.
	 *
	 * @param elBlock the el block
	 */
	private void fixEmptyCBParams(Element elBlock) {
		List<Element> children = elBlock.getChildren("Param");
		for(int i=0; i<children.size(); i++) {
			Element elParam = children.get(i);

			String value = elParam.getText();
			if(value.isEmpty()) {
				String defaultValue = elParam.getAttributeValue("defaultValue");
				if(defaultValue != null && !defaultValue.isEmpty()) {
					elParam.setText(defaultValue);
				}
			}
		}
	}

	//------------------------------------------------------------------------

	public void applyParamChange(double prevValue, double currValue) {
		if(cbParams == null) {
			return;
		}

		List<Element> children = elBlock.getChildren("Param");
		for(int i=0; i<children.size(); i++) {
			Element elParam = children.get(i);

			String key = elParam.getAttributeValue("key");

			if(key.contains("Chart")) {
				continue;
			}

			CParamType realValue = cbParams.get(key);
			if(realValue != null && realValue.value != null) {

				double dRealValue = Double.parseDouble(realValue.value);

				if(dRealValue == prevValue) {
					// we found this variable
					realValue.value = Double.toString(currValue);

					elParam.setText(realValue.value);

					if(realValue.variable != null) {
						elParam.setAttribute("variable", realValue.variable);
					}

					if(realValue.variableType != null) {
						elParam.setAttribute("variableType", realValue.variableType);
					}
				}
			}
		}
	}

	//------------------------------------------------------------------------

	public IBlock getContents() {
		return Contents;
	}

}