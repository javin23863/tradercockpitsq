angular.module('app.resultstabs.strategyconfig', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsTab", 80, {
        title: Ltsq('Strategy config'),
        dataItem: 'strategyConfig',
        templateUrl: '../../../plugins/ResultsStrategyConfig/strategyconfig.html',
        controller: 'StrategyConfigCtrl'
    });
});