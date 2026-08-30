angular.module('app.resultstabs.portfoliocorrelation', ['sqplugin'])
.config(function(sqPluginProvider, SQEventsProvider) {
	
    SQEventsProvider.add("RESET_PORTFOLIO_CORRELATION_TABS", 'reset-portfolio-correlation-tabs');

    sqPluginProvider.plugin("ResultsTab", 70, {
        title: Ltsq('Portfolio correlation'),
        dataItem: 'portfolioCorrelation',
        controller: 'PortfolioCorrelationCtrl',
        templateUrl: '../../../plugins/ResultsPortfolioCorrelation/portfolioCorrelation.html',
        hideAfterInit: true
    });
});