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
package SQ.Blocks.Indicators.Other;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

import SQ.Internal.Indicator;


public class DataLoggingIndy extends Indicator {

	@Parameter
	public TimeDataSeries Tinput;

	@Parameter
	public DataSeries Oinput;

	@Parameter
	public DataSeries Hinput;

	@Parameter
	public DataSeries Linput;

	@Parameter
	public DataSeries Cinput;

	@Parameter
	public DataSeries Vinput;

	@Output(name = "DataLoggingIndy", color = Colors.Red)
	public DataSeries Value;
	
	@Output(name = "DataLoggingIndy", color = Colors.Red)
	public TimeDataSeries TOutput;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	private PrintWriter writer;
	private String fileName;
	private DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("yyyy.MM.dd,HH:mm");
	
	@Override
	public void Initialize() throws TradingException {
		String filePath = MainApp.getDataPath() + "tests/tmp/INDICATOR_ohlcv.csv";
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath), StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new TradingException(e);
		}
	}
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		String time = SQTime.toString(Tinput.get(0), timeFormatter);
		//Log.debug("XXXXXXXXXXXXXXXX  Calling Indy.onBarUpdate() for "+time);

		if(CurrentBar < 1) {
			return;
		}
		
		Value.set(0, Cinput.get(0));
		TOutput.set(0, Tinput.get(0));
		
		time = SQTime.toString(Tinput.get(1), timeFormatter);
		
		writer.println(time+","+Oinput.get(1)+","+Hinput.get(1)+","+Linput.get(1)+","+Cinput.get(1)+","+Vinput.get(1));	}
	
	//------------------------------------------------------------------------

	@Override
	public void Deinitialize() throws TradingException {
		if(writer != null) {
			writer.close();
		}
	}	
}
