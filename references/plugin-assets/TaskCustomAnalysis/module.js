angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 3, {
        taskType: 'CustomAnalysis',
        templateUrl: '../../../plugins/TaskCustomAnalysis/simpleSettings/simpleSettings.html',
        controller: 'SimpleSettingsCACtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});