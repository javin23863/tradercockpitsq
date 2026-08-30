angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("FitnessMethod", 60, {
        key: 'MonteCarloManipulation', 
        templateUrl: '../../../plugins/CrossCheckMonteCarloManipulation/FitnessFunction/mcManipulationFitness.html',
        controller: 'MCManipulationFitnessCtrl'
    });

});