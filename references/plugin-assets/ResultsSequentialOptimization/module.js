angular.module('app.resultstabs.sequentialoptimization', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 12, {
        title: Ltsq('Sequential Optimization Results'),
        dataItem: 'sequentialOptimization',
        templateUrl: '../../../plugins/ResultsSequentialOptimization/sequentialOptimization.html',
        controller: 'ResultsSequentialOptimizationCtrl',
        hideAfterInit: true
    });
});