angular.module('app.resultstabs.portfoliocomposerchart', ['sqplugin']).config(function(sqPluginProvider) {

    sqPluginProvider.plugin("ResultsTab", 999, {
        title: Ltsq('Automatic computation simulations'),
        dataItem: 'portfolioComposerChart',
        templateUrl: '../../../plugins/ResultsPortfolioComposerChart/portfolioComposerChart.html',
        controller: 'PortfolioComposerChartCtrl',
        hideAfterInit: true
    });
});