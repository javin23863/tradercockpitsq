angular.module('app.datasource.mt5api', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        "DataSourceMt5Api", 
        10, 
        {
            title: Ltsq('MT5 import') + ':',
            class: "datasource-mt5api-menu",
            id: "datasource-mt5api-menu",
            source: 'data'
        }
    );
});