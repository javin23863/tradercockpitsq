angular.module('app.resultstabs.optimprof', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 11, {
        title: Ltsq('Optimization profile'),
        dataItem: 'optimizationProf',
        templateUrl: '../../../plugins/ResultsOptimizationProfile/optimizationProfile.html',
        controller: 'OptimizationProfileCtrl',
        hideAfterInit: true
    });
});