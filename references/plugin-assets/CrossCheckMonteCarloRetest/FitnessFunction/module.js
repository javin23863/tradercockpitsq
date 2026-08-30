angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("FitnessMethod", 50, {
        key: 'MonteCarloRetest', 
        templateUrl: '../../../plugins/CrossCheckMonteCarloRetest/FitnessFunction/mcRetestFitness.html',
        controller: 'MCRetestFitnessCtrl'
    });

});