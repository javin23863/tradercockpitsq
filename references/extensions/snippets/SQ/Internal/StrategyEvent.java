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

import java.util.ArrayList;

import org.jdom2.Element;

import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;

/**
 * The Class StrategyEvent.
 */
public class StrategyEvent {

	/** The strategy. */
	private XmlStrategy strategy;
	
	/** The event name. */
	private String eventName;
	
	/** The rules. */
	private Rule[] rules = null; 
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	/**
	 * Instantiates a new strategy event.
	 *
	 * @param strategy the strategy
	 * @param elEvent the el event
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public StrategyEvent(XmlStrategy strategy, Element elEvent) throws XmlStrategyException {
		this.strategy = strategy;
		
		parseXml(elEvent);
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the event name.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		return eventName;
	}

	//------------------------------------------------------------------------

	/**
	 * Evaluate event.
	 *
	 * @param updateEventType the update event type
	 * @param evaluator the evaluator
	 * @throws Exception the exception
	 */
	public void evaluateEvent(int updateEventType, ITradingOptionsEvaluator evaluator) throws Exception {
		if(rules != null) {
			for(Rule rule : rules) {
				rule.evaluateRule(updateEventType, evaluator, eventName);
			}
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Parses the xml.
	 *
	 * @param elEvent the el event
	 * @throws XmlStrategyException the xml strategy exception
	 */
	private void parseXml(Element elEvent) throws XmlStrategyException {
		eventName = elEvent.getAttributeValue("key");
		
		ArrayList<Rule> tempRules = null;
		
		for(Element elRule : elEvent.getChildren()) {
			String snippetName = elRule.getAttributeValue("type");
			
			Rule rule = Rules.get(snippetName);

			rule = rule.newInstance(strategy, elRule);
			
			if(tempRules == null) {
				tempRules = new ArrayList<Rule>();
			}
			
			tempRules.add(rule);
		}
		
		if(tempRules != null) {
			rules = new Rule[tempRules.size()];
		
			int i = 0;
			for(Rule rule : tempRules) {
				rules[i++] = rule;
			}
		
			tempRules.clear();
		}
	}

	//------------------------------------------------------------------------

	/**
	 * Deinitialize.
	 */
	public void deinitialize() {
		if(rules != null) {
			for(int i=0; i<rules.length; i++) {
				rules[i].deinitialize();
			}
		}
	}
	
}
