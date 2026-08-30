angular.module('app.datasource.files', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        "DataSourceFiles", 
        10, 
        {
            title: Ltsq('File import') + ':',
            class: "datasource-files-menu",
            id: "datamanager-datasource-files-menu",
            source: 'data'
        }
    );
});