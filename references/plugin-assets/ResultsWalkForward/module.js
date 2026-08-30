angular.module('app.resultstabs.wf', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 0, {
        title: Ltsq('Walk-Forward Results'),
        dataItem: 'wfResult',
        templateUrl: '../../../plugins/ResultsWalkForward/wfresults.html',
        controller: 'WFResultsCtrl',
        hideAfterInit: true
    });
});