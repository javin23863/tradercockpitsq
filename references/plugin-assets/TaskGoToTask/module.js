angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'GoToTask',
        help: Ltsq('Skips to a defined task when the conditions below are true'),
        templateUrl: '../../../plugins/TaskGoToTask/simpleSettings/simpleSettings.html',
        controller: 'SimpleGoToTaskSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});