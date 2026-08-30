package SQ.TALibIndicators.PercentRank;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.talib.TALibCustomIndicator;

/*
 * https://www.amibroker.com/guide/afl/percentrank.html
 * Returns percent rank (0...100) of the current element of the array within all elements over the specified range.
 * A value of 100 indicates that the current element of the array is the highest for the given lookback range, while a value of 0 indicates that the current value is the lowest for the given lookback range.
 */
public class PercentRank extends TALibCustomIndicator {

	@Parameter(defaultChartIndex=0)
	public int Chart;
	
	@Parameter(category="Default", name="Period", minValue=2, maxValue=10000, defaultValue="14", step=1)
	public int Period;
	
	@Parameter(defaultValue="0")
	@Editor(type=Editors.Selection, values="Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
	public int ComputedFrom;
	
	@Output(name="PercentRank")
	public double[] Value;

	@Override
	public float[][] calculate() {
		float[][] output = new float[1][in0.length];
		
		for(int i=Period; i<in0.length; i++) {
			int count = 0;
			
			for(int j=1; j<=Period; j++) {
				if(in0[i] > in0[i - j]) {
					count++;
				}
			}
						
			output[0][i] = 100f * ((float)count / (float)Period);
		}
		
		return output;
	}
	
	//------------------------------------------------------------------------
	@Override
	protected void init(SettingsMap params, StrategyBase Strategy, float[] o, float[] h, float[] l, float[] c, float[] v, float[] in0, float[] in1) {
		super.init(params, Strategy, o, h, l, c, v, in0, in1);
		
		this.Period = params.getInt("TimePeriod");
	}
}