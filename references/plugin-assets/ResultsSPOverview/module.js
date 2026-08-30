angular.module('app.resultstabs.spoverview', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 11, {
        title: Ltsq('SP overview'),
        dataItem: 'spOverview',
        controller: 'SPOverviewCtrl',
        templateUrl: '../../../plugins/ResultsSPOverview/spOverview.html'
    });
});