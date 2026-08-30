angular.module('app.resultstabs.equitychart', ['sqplugin'])
.config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 30, {
        title: Ltsq('Equity chart'),
        dataItem: 'equityChart',
        controller: 'EquityChartCtrl',
        templateUrl: '../../../plugins/ResultsEquityChart/equityChart.html'
    });
});