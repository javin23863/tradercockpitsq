angular.module('app.datasource.td', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        "DataSourceTD", 
        10, 
        {
            title: Ltsq('TickDownloader import') + ':',
            class: "datasource-td-menu",
            id: "datasource-td-menu",
            source: 'data'
        }
    );
});