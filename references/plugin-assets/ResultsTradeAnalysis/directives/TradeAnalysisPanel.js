angular.module('app.resultstabs.tradeanalysis').directive('tradeAnalysisPanel', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsTradeAnalysis/directives/tradeAnalysisPanel.html',
        scope : {
            types: '=',
            instance: '=',
            width: '@',
            height: '@',
        },
        controller: 'TradeAnalysisPanelCtrl'
    }
});