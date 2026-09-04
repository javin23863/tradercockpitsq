# Switching logs to debug mode

- Source: <https://strategyquant.com/doc/strategyquant/switching-logs-to-debug-mode/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

When StrategyQuant X is not able to start up and support team will ask you for debug log here the tutorial on how to do that:

Open this config file

```
StrategyQuantX install folder/internal/web/SQUANT/log_config.xml
change INFO 
<root level="INFO">
to DEBUG
<root level="DEBUG">
 
edited config file: 
<configuration>

      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender"> 
        <encoder>
      		<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    	</encoder>
  	</appender>
  
  	<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
      	<!-- create new log file every day -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <FileNamePattern>./user/log/StrategyQuant/log_%d{yyyy_MM_dd}.log</FileNamePattern>
        </rollingPolicy>
          	
     	<encoder>
       		<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
     	</encoder>
   	</appender>

  	<root level="DEBUG">
        <appender-ref ref="STDOUT" />
    	<appender-ref ref="FILE" />
    </root>
    
    <logger name="oshi.util.platform.windows.WmiUtil">
        <level value="INFO"/>
    </logger>

    <logger name="org.eclipse.jetty">
        <level value="INFO"/>
    </logger>

</configuration>
```
