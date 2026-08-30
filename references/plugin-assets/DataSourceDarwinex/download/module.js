angular.module('app.datasource.darwinex.download', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDarwinex',
        20,
        {
            title: Ltsq('Darwinex Tick Data') + ':' + Ltsq('Download data for existing symbols'),
            templateUrl: '../../../plugins/DataSourceDarwinex/download/downloadPopup.html',
            controller: 'darwinexDownloadPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-darwinex-download"
        }
    );
});