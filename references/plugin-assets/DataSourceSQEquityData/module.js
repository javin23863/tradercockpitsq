angular.module('app.datasource.sqEquityData', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        "DataSourceSQEquityData", 
        20, 
        {
            title: Ltsq('SQ Equity data') + ':',
            class: "datasource-sqequitydata-menu",
            id: "datamanager-datasource-sqequitydata-menu",
            source: 'data'
        }
    );
});