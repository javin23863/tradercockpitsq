angular.module('app.resultstabs.tradeanalysis', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 40, {
        title: Ltsq('Trade analysis'),
        dataItem: 'tradeAnalysis',
        controller: 'TradeAnalysisCtrl',
        templateUrl: '../../../plugins/ResultsTradeAnalysis/views/tradeAnalysis.html'
    });
});