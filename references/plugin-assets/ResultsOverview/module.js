angular.module('app.resultstabs.overview', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 10, {
        title: Ltsq('Overview'),
        dataItem: 'overview',
        templateUrl: '../../../plugins/ResultsOverview/overview.html',
        controller: 'ResultsOverviewCtrl'
    });
});