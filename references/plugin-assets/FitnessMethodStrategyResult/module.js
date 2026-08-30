angular.module('app.fitnessStrategyResult', ['sqplugin']).config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("FitnessMethod", 10, {
        title: Ltsq('Main data backtest'),
        key: 'ComputeFromStrategyResult', 
        templateUrl: '../../../plugins/FitnessMethodStrategyResult/fitnessMethodStrategyResult.html',
        controller: 'FitnessMethodStrategyResultCtrl'
    });

});