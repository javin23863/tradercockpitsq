angular.module('app.resultstabs.tradelist', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 20, {
        title: Ltsq('List of trades'),
        dataItem: 'tradeList',
        templateUrl: '../../../plugins/ResultsTradeList/tradeList.html',
        controller: 'ResultsTradelistCtrl'
    });
});