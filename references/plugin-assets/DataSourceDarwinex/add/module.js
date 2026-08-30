angular.module('app.datasource.darwinex').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDarwinex',
        0,
        {
            title: Ltsq('Darwinex Tick Data') + ':' + Ltsq('Add Darwinex data'),
            templateUrl: '../../../plugins/DataSourceDarwinex/add/addPopup.html',
            controller: 'darwinexAddPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-darwinex-add"
        }
    );
});