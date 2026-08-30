angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("FitnessMethod", 20, {
        key: 'RetestOnAdditionalMarkets', 
        templateUrl: '../../../plugins/CrossCheckRetestOnAdditionalMarkets/FitnessFunction/portfolioFitness.html',
        controller: 'PortfolioFitnessCtrl'
    });

});