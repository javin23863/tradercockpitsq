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
package SQ.MonteCarlo.Retest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Randomize strategy parameters Customizable", display="Customizable Randomize strategy parameters, with probability #Probability# % and max change #MaxChange# %")
public class RandomizeStrategyParametersCustomizable extends MonteCarloRetest {
	public static final Logger Log = LoggerFactory.getLogger(RandomizeStrategyParametersCustomizable.class);
	
	@Parameter(name="Probability", defaultValue="100", minValue=1, maxValue=100, step=1)
	@Help("% Probability of parameter change")
	public int Probability;
	
	@Parameter(name="Max change", defaultValue="50", minValue=1, maxValue=500, step=1)
	@Help("Max % change of parameter")
	public int MaxChange;
	
	@Parameter(name="Symmetric parameters", defaultValue="true")
	@Help("If true, it uses symmetric parameters - the parameters will be shared for long and short side. Otherwise, the parameters for long and short side will be independent.")
	public boolean Symmetric;

	@Parameter(name="Period", defaultValue="true")
	@Help("Randomize Period")
	public boolean Period;

	@Parameter(name="Shift", defaultValue="false")
	@Help("Randomize Shift")
	public boolean Shift;

	@Parameter(name="Constant", defaultValue="false")
	@Help("Randomize Constant")
	public boolean Constant;

	@Parameter(name="Other Param", defaultValue="false")
	@Help("Randomize Other Param")
	public boolean OtherParam;

	@Parameter(name="Exit Used", defaultValue="false")
	@Help("Randomize Exit Used")
	public boolean ExitUsed;

	@Parameter(name="Exit Unused", defaultValue="false")
	@Help("Randomize Exit Unused")
	public boolean ExitUnused;

	@Parameter(name="Boolean", defaultValue="false")
	@Help("Randomize Boolean")
	public boolean Boolean;

	@Parameter(name="Trading Options", defaultValue="false")
	@Help("Randomize Trading Options")
	public boolean TradingOptions;

	private ValuesMap paramTypes = new ValuesMap();
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public RandomizeStrategyParametersCustomizable() {
		super(MonteCarloTestTypes.ModifySettings);
	}
	
	//------------------------------------------------------------------------	
	
	public void modifySettings(IRandomGenerator rng, SettingsMap settings) throws Exception {
		StrategyBase strategy = StrategyBase.getStrategy(settings);

		paramTypes.set(ParametrizationTypes.ParamTypePeriod, Period);
		paramTypes.set(ParametrizationTypes.ParamTypeShift, Shift);
		paramTypes.set(ParametrizationTypes.ParamTypeConstant, Constant);
		paramTypes.set(ParametrizationTypes.ParamTypeOtherParam, OtherParam);
		paramTypes.set(ParametrizationTypes.ParamTypeExitUsed, ExitUsed);
		paramTypes.set(ParametrizationTypes.ParamTypeExitUnused, ExitUnused);
		paramTypes.set(ParametrizationTypes.ParamTypeBoolean, Boolean);	
		paramTypes.set(ParametrizationTypes.ParamTypeTradingOptions, TradingOptions);		
		
		strategy.transformToVariables(Symmetric, paramTypes);

		Variables vars = strategy.variables();
		vars.sortByName();
		
		if(vars.size() == 0) {
			return;
		}
		
		double dblProbability = ((double) Probability/ 100.0d);

		int tries = 0;
		while(true) {
			
			int varsChanged = modifyParameters(vars, dblProbability, rng);
			if(varsChanged > 0) {
				break;
			}
			
			tries++;
			if(tries > 10) {
				break;
			}
		}
	}

	//------------------------------------------------------------------------	

	private int modifyParameters(Variables vars, double dblProbability, IRandomGenerator rng) {
		int varsChanged = 0;
		
		int valuesThatCanChange = 0;
		
		for(int i=0; i<vars.size(); i++) {
			Variable variable = vars.get(i);
			
			if(!isCorrectType(variable)) {
				// it is variable of a different type
				continue;
			}
			
			if(variable.getName().contains("Magic")) {
				continue;
			}
			
			valuesThatCanChange++;
		}
		
		if(valuesThatCanChange > 0) {

			int cycles = 0;
			
			while(true) {
				varsChanged = changeSomeVars(vars, dblProbability, rng);

				// we must change at least one variable
				if(varsChanged > 0) {
					break;
				}
				
				if(cycles > 100) {
					// protection against infinite cycle
					break;
				}
			}
		}

		return varsChanged;
	}

	//------------------------------------------------------------------------	

	private int changeSomeVars(Variables vars, double dblProbability, IRandomGenerator rng) {
		int varsChanged = 0;
		
		for(int i=0; i<vars.size(); i++) {
			Variable variable = vars.get(i);

			if(!isCorrectType(variable)) {
				// it is variable of a different type
				continue;
			}
			
			if(variable.getName().contains("Magic")) {
				continue;
			}
			
			if(!rng.probability(dblProbability)) {
				// we shouldn't change this value
				continue;
			}
			
			int varType = variable.getInternalType();
			if(varType == Variable.TypeBoolean) {
				variable.setValue(!variable.getValueAsBoolean());
				varsChanged++;
				
			} else if(varType == Variable.TypeInt || varType == Variable.TypeDouble) {
				
				double value = variable.getValueAsDouble();
			
				// randomly determine change
				double pctChange = ((double) (1+rng.nextInt(MaxChange)) / 100.0d);
				
				double change = value * pctChange;

				double newValue = (rng.nextInt(2) == 0 ? value + change : value - change);

				if(varType == Variable.TypeInt) {
					newValue = (int) Math.round(newValue);
				}
				
				if(value != newValue) {
					varsChanged++;
				}

				if(varType == Variable.TypeInt) {
					variable.setValue((int) newValue);
				} else {
					variable.setValue(newValue);
				}
			}
		}
		
		return varsChanged;
	}

	//------------------------------------------------------------------------	

	private void printVars(String string, Variables vars, int varsChanged) {
		Log.info("------------------------------------");
		Log.info(string+", Changed: "+varsChanged);
		Log.info("------------------------------------");
		
		for(int i=0; i<vars.size(); i++) {
			Variable variable = vars.get(i);
			
			Log.info("Var #{} : {}", i, variable.toString());
		}
	}

	//------------------------------------------------------------------------	

	private boolean isCorrectType(Variable variable) {
		if(variable == null || variable.getParamType() == null) {
			return false;
		}
		
		return (paramTypes.getBoolean(variable.getParamType(), false) == true);
	}		

	/**
	 * Gets the clone.
	 *
	 * @return the clone
	 * @throws Exception the exception
	 */
	@Override
	public RandomizeStrategyParametersCustomizable getClone() throws Exception {
		RandomizeStrategyParametersCustomizable mc = new RandomizeStrategyParametersCustomizable();

		mc.Probability = this.Probability;
		mc.MaxChange = this.MaxChange;
		mc.Symmetric = this.Symmetric;
		mc.Period = this.Period;
		mc.Shift = this.Shift;
		mc.Constant = this.Constant;
		mc.OtherParam = this.OtherParam;
		mc.ExitUsed = this.ExitUsed;
		mc.ExitUnused = this.ExitUnused;
		mc.Boolean = this.Boolean;
		mc.TradingOptions = this.TradingOptions;


		mc.setParams(this.getParams());
		
		return mc;
	}


}