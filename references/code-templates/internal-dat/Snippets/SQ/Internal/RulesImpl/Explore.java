package SQ.Internal.RulesImpl;

import java.util.ArrayList;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;

import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Explore extends Rule {	
	public static final Logger Log = LoggerFactory.getLogger("Explore");

	private ObjectArrayList<IBlock> blocks = null;
	private ObjectArrayList<String> names = null;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void evaluateRule(int updateEventType, ITradingOptionsEvaluator evaluator, String event) throws Exception {
		
		if(Strategy.Explore == null) {
			Strategy.Explore = new com.strategyquant.tradinglib.explore.Explore(names);
		}

		String symbol = Strategy.getSymbol();
		boolean eval = false;
		long time = 0;
		
		if(Strategy.isStockpicker()) {				
			//entry ------------------------------------------------------------------------------------------
			if(Strategy.Stockpicker.entryType() == Strategy.Stockpicker.strategyTriggeredAt()) {
				eval = true;
				time = Strategy.Stockpicker.data.getCurrentTime();
			}
		} else {
			eval = true;
			time = Strategy.MarketData.TimeCurrent();
		}
		
		if(eval) {			
			double result;
			
			for(int i=0; i < this.blocks.size(); i++) {				
				try {
					IBlock block = this.blocks.get(i);				
					result = block.evaluateBlock();
				} catch(Exception e) {
					result = 0;
				}
				
				Strategy.Explore.add(symbol, time, i, result);
			}
		}
	}
		
	@Override
	protected void parseXml(Element elRule) throws BlockDefinitionException {
		this.blocks = new ObjectArrayList<IBlock>();
		this.names = new ObjectArrayList<String>();
		
		for(Element elRulePart : elRule.getChildren()) {
			if(elRulePart.getName().contains("Description")) {
				continue;
			}
			
			int i=0;
			
			for(Element elBlock : elRulePart.getChildren()) {
				if(!elBlock.getName().equals("Item")) {
					throw new BlockDefinitionException("Block has an unallowed name '"+elBlock.getName()+"'");
				}
				
				IBlock block = Blocks.getBlockObject(elBlock.getAttributeValue("key"), Strategy, elBlock);
				
				BuildingBlock bb = block.getClass().getAnnotation(BuildingBlock.class);
				if(bb != null) {
					block.setReturnType(bb.returnType());
				}
				
				String exploreName = elBlock.getAttributeValue("exploreName");
				String name = exploreName==null ? "Value " + (i+1) : exploreName;

				this.blocks.add(block);
				this.names.add(name);
				
				i++;
			}
		}		
	}

}