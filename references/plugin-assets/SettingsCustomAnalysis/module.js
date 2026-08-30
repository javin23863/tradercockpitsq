//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Custom analysis'),
        help: Ltsq('Performs the specified custom analysis on all strategies of given databank'),
        helpURL: 'custom-analysis',
        task: 'CustomAnalysis',
        configElemName: 'CustomAnalysis',
        dataItem: 'tm-custom-analysis',
        templateUrl: '../../../plugins/SettingsCustomAnalysis/settingsCustomAnalysis.html',
        controller: 'SettingsCustomAnalysisCtrl',
        function(settingsElement, injector){
            return Ltsq('Custom analysis');
        }
    });
});