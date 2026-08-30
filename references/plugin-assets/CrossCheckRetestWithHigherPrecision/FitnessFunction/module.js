angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("FitnessMethod", 40, {
        key: 'RetestWithHigherPrecision', 
        templateUrl: '../../../plugins/CrossCheckRetestWithHigherPrecision/FitnessFunction/ccHigherPrecision.html',
        controller: 'CCHigherPrecisionCtrl'
    });

});