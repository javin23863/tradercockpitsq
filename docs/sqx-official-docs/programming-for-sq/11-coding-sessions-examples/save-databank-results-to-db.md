# Save databank results to DB

- Source: <https://strategyquant.com/doc/programming-for-sq/save-databank-results-to-db/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

This example is about saving databank contents to a database, implemented as CustomAnalysis snippet.

Note! There was a small bug in the original extension – it used a package that wasn’t yet available in released version of SQ X. Please redownload the snippet.

It uses SQLite DB for simplicity, but there is no problem to use MySql or MariaDB or any other database, because it uses JDBC database connection which is a Java standard when connecting and communicating with databases.  
When using another database you’ll need to:

- change the JDBC connection string in ***createDBConnection()*** method
- it might be necessary to add the JDBC library for your target database – SQLite is already included in SQX, but databases like MySql or MariaDB are not – so you’ll need to find its JDBC library (something like <https://dev.mysql.com/downloads/connector/j/> and add it’s JAR file(s) to {StrategyQuant installation}/user/libs folder.

quite complex, because it saves the exact databank view that you see in the UI – if you don’t want to use view you can simply get values like Net profit, Number of trades, etc. from ResultsGroup and save them on your own.

The main logic is actually very simple:

```
@Override
public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
    Databank databank = ProjectEngine.get(project).getDatabanks().get(databankName);
    if(databank == null){
        throw new Exception("Databank " + project + "/" + databankName + " doesn't exist");
    }

    DatabankTableView view = databank.getView();

    String tableName = getTableName(project, task, databankName);

    try (Connection dbConnection = createDBConnection(tableName, view)) {
        dbConnection.setAutoCommit(false);

        try (Statement stmt = dbConnection.createStatement()) {
            for (int a = 0; a < databankRG.size(); a++) {
                ResultsGroup rg = databankRG.get(a);
                writeData(rg, view, tableName, stmt);
            }
        }

        dbConnection.commit();
    }

    return databankRG;
}
```

The code will create DB connection, create Statement, then go through ResultGroups stored in databank and save each one of them to DB using standard SQL statement.

Full code of the snippet (it can be also downloaded in the attachment to this post):

```
package SQ.CustomAnalysis;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.project.ProjectEngine;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class SaveDatabankToDB extends CustomAnalysisMethod {

    public static final Logger Log = LoggerFactory.getLogger("SaveDatabankToDB");

    private static final String TABLE_NAME_SEPARATOR = "__";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd-HHmm");

    public static final String TABLE_NAME = "databankData";

    private static final String STRATEGY_COLUMN_NAME = "strategyName";

    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------

    /**
     * Instantiates a new custom analysis method.
     */
    public SaveDatabankToDB() {
        super("SaveDatabankToDB", TYPE_PROCESS_DATABANK);
    }

    //------------------------------------------------------------------------

    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {
        return true;
    }

    //------------------------------------------------------------------------

    @Override
    public ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG) throws Exception {
        Databank databank = ProjectEngine.get(project).getDatabanks().get(databankName);
        if(databank == null){
            throw new Exception("Databank " + project + "/" + databankName + " doesn't exist");
        }

        DatabankTableView view = databank.getView();

        String tableName = getTableName(project, task, databankName);

        try (Connection dbConnection = createDBConnection(tableName, view)) {
            dbConnection.setAutoCommit(false);

            try (Statement stmt = dbConnection.createStatement()) {
                for (int a = 0; a < databankRG.size(); a++) {
                    ResultsGroup rg = databankRG.get(a);
                    writeData(rg, view, tableName, stmt);
                }
            }

            dbConnection.commit();
        }

        return databankRG;
    }

    //------------------------------------------------------------------------

    public static String getOutputFolder(){
        return "C:/SQDatabankExports";
    }

    //------------------------------------------------------------------------

    private Connection createDBConnection(String tableName, DatabankTableView view) throws Exception {
        Connection c = null;

        try {
            Class.forName("org.sqlite.JDBC");
            String folderPath = getOutputFolder();

            SQUtils.ensureDirExists(folderPath);

            String dbPath = String.format("%s/%s.db", folderPath, tableName);
            String connectionString = String.format("jdbc:sqlite:%s", dbPath);

            c = DriverManager.getConnection(connectionString);

            tryCreateTable(c, tableName, view);
        }
        catch (Exception e ) {
            Log.error("Creating database connection failed", e);
            throw e;
        }

        return c;
    }

    //------------------------------------------------------------------------

    public static String getTableName(String projectName, String taskName, String databankName){
        String dateTime = dateTimeFormatter.print(System.currentTimeMillis());
        return dateTime + TABLE_NAME_SEPARATOR + projectName + TABLE_NAME_SEPARATOR + taskName + TABLE_NAME_SEPARATOR + databankName;
    }

    //------------------------------------------------------------------------

    private void tryCreateTable(Connection connection, String tableName, DatabankTableView view) throws Exception {
        if(view == null){
            throw new Exception("Databank view is null");
        }

        Statement stmt = null;
        try {
            StringBuilder sb = new StringBuilder(STRATEGY_COLUMN_NAME).append(" TEXT PRIMARY KEY");

            ColumnNamesResolver columnNamesResolver = new ColumnNamesResolver();

            for(int a=0; a<view.columns.size(); a++){
                DatabankTableColumnEntry column = view.columns.get(a);
                String columnName = columnNamesResolver.getColumnName(column);

                sb.append(",");
                sb.append(columnName);
                sb.append(" TEXT");
            }

            String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s);", TABLE_NAME, sb);

            stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        }
        finally {
            if(stmt != null) {
                stmt.close();
            }
        }
    }

    //------------------------------------------------------------------------

    private void writeData(ResultsGroup rg, DatabankTableView view, String tableName, Statement stmt) throws SQLException {
        StringBuilder columnNames = new StringBuilder();
        StringBuilder values = new StringBuilder();

        ColumnNamesResolver columnNamesResolver = new ColumnNamesResolver();

        columnNames.append(STRATEGY_COLUMN_NAME);
        values.append("'").append(rg.getName()).append("'");

        for (int a = 0; a < view.columns.size(); a++) {
            DatabankTableColumnEntry column = view.columns.get(a);
            String columnName = columnNamesResolver.getColumnName(column);
            String value = "N/A";

            try {
                value = column.tableColumn.exportValue(rg, null, Directions.Both, PlTypes.Money, SampleTypes.FullSample);
            } catch (Exception e) {
                Log.error("Error getting value for strategy + " + rg.getName() + " and column " + columnName, e);
            }

            columnNames.append(",").append(columnName);
            values.append(",").append("'").append(value).append("'");
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", TABLE_NAME, columnNames, values);
        stmt.executeUpdate(sql);
    }

    //------------------------------------------------------------------------

    private class ColumnNamesResolver {

        private HashMap<String, Integer> columnNameCounts = new HashMap<>();

        public String getColumnName(DatabankTableColumnEntry columnEntry){
            String columnName = columnEntry.tableColumn.getClass().getSimpleName();
            String finalColumnName = null;

            //following code takes care of multiple columns with the same name inside databank view
            Integer columnCount = columnNameCounts.get(columnName);
            if(columnCount != null){
                columnCount += 1;
                finalColumnName = columnName + columnCount;
            }
            else {
                columnCount = 1;
                finalColumnName = columnName;
            }

            columnNameCounts.put(columnName, columnCount);

            return finalColumnName;
        }

    }
}
```
