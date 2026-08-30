angular.module('app.datasource.sqFuturesData', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        "DataSourceSQFuturesData", 
        20, 
        {
            title: Ltsq('SQ Futures data') + ':',
            class: "datasource-sqfuturesdata-menu",
            id: "datamanager-datasource-sqfuturesdata-menu",
            source: 'data'
        }
    );
});