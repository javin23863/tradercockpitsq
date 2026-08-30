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

import org.jdom2.Element;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.data.CustomDataIndyCache;
import com.strategyquant.tradinglib.data.CustomDataIndyMap;

/**
 * The Class CDataIndy.
 */
public class CDataIndy extends ValueBlock {


	/** The Value. */
	@Parameter
	public int Value = 0;

	/** The Shift. */
	@Parameter
	public int Shift;
	
	/** The Name. */
	private String Name;

	/** The map. */
	private CustomDataIndyMap map;

	/** The Data type. */
	private String DataType;
	
	/** The el block. */
	private Element elBlock;
	
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
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		if(map == null) {
			Log.error("External Indicator {} doesn't have data!", Name);
			return 0;
		}
		
		long barTime = Strategy.MarketData.Chart(0).Time(relativeShift + Shift);
		double value = map.getTime(barTime);
		
		return value;
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
		CDataIndy block = (CDataIndy) clone(true, strategy);
		
		block.initialize(strategy, elBlock);
		
		block.elBlock = elBlock.clone();
		
		block.map = CustomDataIndyCache.getMapForIndy(elBlock);
		
		if(block.map != null) {
			block.Name = block.map.getName();
			block.Value = block.map.getValueIndex();
			block.Value = block.map.getValueIndex();
		}
		
		return block;
	}

	//------------------------------------------------------------------------

	/**
	 * Clone C data indy.
	 *
	 * @return the c data indy
	 */
	public CDataIndy cloneCDataIndy() {
		CDataIndy newI = new CDataIndy();
		
		newI.Name = Name;
		newI.Shift = Shift;
		newI.Value = Value;
		newI.map = map;
		newI.elBlock = elBlock != null ? elBlock.clone() : null;
		
		return newI;
	}	
	
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
		return cloneCDataIndy();
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
		if(elBlock != null) {
			return elBlock;
		}
		
		/*
<Item key="CDataIndy_MyCustIndy" cdataType="1" generated="random" randomId="RandomConditionLong">
  <Param key="#Shift#" controlType="jspinnerVar" type="int">3</Param>
  <Param key="#Value#" controlType="combo" type="int">1</Param>
</Item>
		 */
		
		elBlock = new Element("Item");
		elBlock.setAttribute("key", "CDataIndy_"+Name);
		
		Element elParam = new Element("Param");
		elParam.setAttribute("key", "#Shift#");
		elParam.setAttribute("controlType", "jspinnerVar");
		elParam.setAttribute("type", "int");
		elParam.setText(Integer.toString(Shift));
		elBlock.addContent(elParam);

		elParam = new Element("Param");
		elParam.setAttribute("key", "#Value#");
		elParam.setAttribute("controlType", "combo");
		elParam.setAttribute("type", "int");
		elParam.setText(Integer.toString(Value));
		elBlock.addContent(elParam);

		return elBlock;
	}

	//------------------------------------------------------------------------

	/**
	 * Negate value index.
	 */
	public void negateValueIndex() {
		if(Name == null) {
			return;
		}
		
		CustomDataInfo dataInfo = CustomDataManager.getDataInfo(Name);
		if(dataInfo == null) {
			return;
		}
		
		if(dataInfo.values == 2) {
			Value = (Value == 1 ? 0 : 1);
		}
	}		
}