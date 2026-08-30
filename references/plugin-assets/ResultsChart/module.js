angular.module('app.resultstabs.chart', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 50, {
        title: Ltsq('Trades on chart'),
        dataItem: 'tradesOnChart',
        templateUrl: '../../../plugins/ResultsChart/chart.html',
        controller: 'ResultsChartCtrl',
        hideAfterInit: true
    });
    
});