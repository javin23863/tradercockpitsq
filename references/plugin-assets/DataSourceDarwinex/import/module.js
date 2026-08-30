angular.module('app.datasource.darwinex.import', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDarwinex',
        10,
        {
            title: Ltsq('Darwinex Tick Data') + ':' + Ltsq('Import data'),
            templateUrl: '../../../plugins/DataSourceDarwinex/import/importPopup.html',
            controller: 'darwinexImportPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-darwinex-import"
        }
    );
});