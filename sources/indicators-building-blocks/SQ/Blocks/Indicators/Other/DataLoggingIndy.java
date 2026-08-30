package SQ.Blocks.Indicators.Other;

import SQ.Internal.Indicator;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
   @Output(name = "DataLoggingIndy", color = "#FF0000")
   public DataSeries Value;
   @Output(name = "DataLoggingIndy", color = "#FF0000")
   public TimeDataSeries TOutput;
   private PrintWriter writer;
   private String fileName;
   private DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("yyyy.MM.dd,HH:mm");

   public void Initialize() throws TradingException {
      String var1 = MainApp.getDataPath() + "tests/tmp/INDICATOR_ohlcv.csv";

      try {
         this.writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(var1), StandardCharsets.UTF_8)));
      } catch (IOException var3) {
         throw new TradingException(var3);
      }
   }

   protected void OnBarUpdate() throws TradingException {
      String var1 = SQTime.toString(this.Tinput.get(0), this.timeFormatter);
      if (this.CurrentBar >= 1) {
         this.Value.set(0, this.Cinput.get(0));
         this.TOutput.set(0, this.Tinput.get(0));
         var1 = SQTime.toString(this.Tinput.get(1), this.timeFormatter);
         this.writer
            .println(
               var1 + "," + this.Oinput.get(1) + "," + this.Hinput.get(1) + "," + this.Linput.get(1) + "," + this.Cinput.get(1) + "," + this.Vinput.get(1)
            );
      }
   }

   public void Deinitialize() throws TradingException {
      if (this.writer != null) {
         this.writer.close();
      }
   }
}
