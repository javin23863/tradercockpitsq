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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ICustomClasses;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;

/**
 * The Class Rules.
 */
public class Rules {
	
	/** The Constant Log. */
	public static final Logger Log = LoggerFactory.getLogger("Rules");

	/** The instance. */
	private static Rules instance;
	
	/** The map classes. */
	private Int2ObjectOpenHashMap<Rule> mapClasses = new Int2ObjectOpenHashMap<Rule>();
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	/**
	 * Gets the single instance of Rules.
	 *
	 * @return single instance of Rules
	 * @throws XmlStrategyException the xml strategy exception
	 */
    static Rules getInstance() throws XmlStrategyException {
        if (instance == null) {
            instance = new Rules();
        }
        
        return instance;
    }

	//------------------------------------------------------------------------

    /**
	 * Instantiates a new rules.
	 *
	 * @throws XmlStrategyException the xml strategy exception
	 */
    private Rules() throws XmlStrategyException {
    	initClassesMap("Internal/RulesImpl");
    }
	
	//------------------------------------------------------------------------

	/**
	 * Inits the classes map.
	 *
	 * @param dirName the dir name
	 * @throws XmlStrategyException the xml strategy exception
	 */
	private void initClassesMap(String dirName) throws XmlStrategyException {
		mapClasses.clear();
		
   		CustomClassesLoader classLoader = new CustomClassesLoader(dirName);
    		
   		while(classLoader.hasNext()) {
   		   String clazz = classLoader.getNext();
 			   
		   Rule rule = (Rule) classLoader.createInstance(clazz);;

		   if(rule == null) {
			   throw new XmlStrategyException("Cannot instantiate rule class '"+clazz+"'");
		   }

		   mapClasses.put(rule.getClass().getSimpleName().hashCode(), rule);
   		}
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the.
	 *
	 * @param snippetName the snippet name
	 * @return the rule
	 * @throws XmlStrategyException the xml strategy exception
	 */
	public static Rule get(String snippetName) throws XmlStrategyException {
		return getInstance()._get(snippetName);
	}

	//------------------------------------------------------------------------

	/**
	 * Gets the.
	 *
	 * @param snippetName the snippet name
	 * @return the rule
	 * @throws XmlStrategyException the xml strategy exception
	 */
	private Rule _get(String snippetName) throws XmlStrategyException {
		int classHash = snippetName.hashCode();
		
		if(!mapClasses.containsKey(classHash)) {
			throw new XmlStrategyException("Cannot find rule with class '"+snippetName+"'");
		}
		
		return mapClasses.get(classHash);
	}
	
	//------------------------------------------------------------------------

	public static void reload() throws Exception {
		instance = new Rules();
	}

}
